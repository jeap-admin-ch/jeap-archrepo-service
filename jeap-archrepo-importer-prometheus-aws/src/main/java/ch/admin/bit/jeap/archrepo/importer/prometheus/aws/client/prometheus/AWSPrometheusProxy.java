package ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.PrometheusHelper;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.PrometheusException;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponse;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * This class can be used to query prometheus metrics trough amp.
 */

@Component
@Slf4j
public class AWSPrometheusProxy {
    private static final String QUERY_URL_PATTERN = "%s/workspaces/%s/api/v1/query_range";

    private final ObjectMapper objectMapper;

    private final AWSConnectorProperties awsConnectorProperties;

    private final URI ampUri;

    public AWSPrometheusProxy(AWSConnectorProperties connectorProperties) {
        this.objectMapper = new ObjectMapper();
        this.awsConnectorProperties = connectorProperties;
        try {
            this.ampUri = new URI(String.format(QUERY_URL_PATTERN,
                    connectorProperties.getHost(),
                    connectorProperties.getWorkspace()));
        } catch (URISyntaxException e) {
            throw PrometheusException.uriSyntaxException(e);
        }
    }


    public List<PrometheusQueryResponseResult> queryRange(String queryString, int rangeDays) {
        PrometheusQueryResponse response;
        try {
            response = callAmp(PrometheusHelper.queryParameters(queryString, rangeDays));

        } catch (Exception e) {
            log.warn("Error in Grafana call", e);
            throw PrometheusException.wrapConnectionException(e);
        }
        PrometheusHelper.validateResponse(response);
        return response.getData().getResult();
    }

    private AwsSessionCredentials retrieveAwsSessionCredentials() {
        log.trace("Call AssumeRole with roleArn {}", awsConnectorProperties.getRoleArn());

        AssumeRoleResponse assumeRoleResponse;
        try (StsClient stsClient = StsClient.builder()
                .credentialsProvider(null)
                .region(Region.EU_CENTRAL_1)
                .build()) {

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(awsConnectorProperties.getRoleArn())
                    .roleSessionName(awsConnectorProperties.getRoleSessionName())
                    .build();

            assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
        }

        Credentials sessionCredentials = assumeRoleResponse.credentials();
        return AwsSessionCredentials.create(
                sessionCredentials.accessKeyId(),
                sessionCredentials.secretAccessKey(),
                sessionCredentials.sessionToken());
    }

    private PrometheusQueryResponse callAmp(MultiValueMap<String, String> queryParameters) throws IOException {

        // Create HTTP request
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.POST)
                .uri(ampUri)
                .rawQueryParameters(queryParameters)
                .build();

        // Sign the request
        Aws4Signer signer = Aws4Signer.create();
        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                .awsCredentials(retrieveAwsSessionCredentials())
                .signingName("aps")
                .signingRegion(Region.EU_CENTRAL_1)
                .build();

        SdkHttpFullRequest signedRequest = signer.sign(request, signerParams);

        HttpExecuteRequest httpExecuteRequest = HttpExecuteRequest.builder()
                .request(signedRequest)
                .build();


        HttpExecuteResponse httpExecuteResponse;
        try (SdkHttpClient httpClient = UrlConnectionHttpClient.builder().build()) {
            httpExecuteResponse = httpClient
                    .prepareRequest(httpExecuteRequest)
                    .call();
        }

        SdkHttpResponse sdkHttpResponse = httpExecuteResponse.httpResponse();
        log.trace("Response Code: {}", sdkHttpResponse.statusText());


        if (httpExecuteResponse.responseBody().isPresent()) {
            return getFromResponse(httpExecuteResponse.responseBody().get());
        }

        return null;

    }

    private PrometheusQueryResponse getFromResponse(AbortableInputStream abortableInputStream) throws IOException {
        String response = new String(abortableInputStream.readAllBytes(), StandardCharsets.UTF_8);
        log.trace("response {}", response);
        PrometheusQueryResponse prometheusQueryResponse = objectMapper.readValue(response, PrometheusQueryResponse.class);
        log.trace("Results {}", prometheusQueryResponse.getData().getResult());
        return prometheusQueryResponse;
    }

}
