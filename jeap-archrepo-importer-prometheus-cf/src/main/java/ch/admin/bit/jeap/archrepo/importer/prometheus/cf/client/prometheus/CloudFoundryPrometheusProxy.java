package ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.PrometheusHelper;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.PrometheusException;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponse;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * This class can be used to query prometheus metrics trough grafana. We will therefore use the Data Source API of
 * Grafana that allows us to proxy requests to a specific datasource through grafana.
 * <p>
 * See <a href="https://grafana.com/docs/grafana/latest/http_api/data_source/#data-source-proxy-calls">Grafana Proxy API</a>
 * See <a href="https://prometheus.io/docs/prometheus/latest/querying/api/#querying-metadata">Prometheus Query API</a>
 * <p>
 * Therewith we can use the user management features of grafana and the filtering that on prometheus that is already
 * in place and do not rely directly on additional prometheus infrastructure
 */

@Component
@Slf4j
public class CloudFoundryPrometheusProxy {
    private static final String QUERY_URL_PATTERN = "%s/api/datasources/proxy/%s/api/v1/query_range";

    private final List<RestClient> restClients;

    public CloudFoundryPrometheusProxy(CloudFoundryConnectorProperties cloudFoundryConnectorProperties, RestClient.Builder restClientBuilder) {
        restClients = new ArrayList<>();

        for (CloudFoundryHostProperties host : cloudFoundryConnectorProperties.getHosts()) {
            log.info("Configuring GrafanaClient for host '{}'", host.getHost());
            String baseUrl = String.format(QUERY_URL_PATTERN,
                    host.getHost(),
                    cloudFoundryConnectorProperties.getDatasource());
            ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                    .withReadTimeout(cloudFoundryConnectorProperties.getTimeout()));
            restClients.add(restClientBuilder
                    .requestFactory(requestFactory)
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + host.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .build());
        }
    }

    public List<PrometheusQueryResponseResult> queryRange(String queryString, int rangeDays) {
        List<PrometheusQueryResponseResult> results = new ArrayList<>();

        for (RestClient restClient : this.restClients) {
            results.addAll(queryRange(restClient, queryString, rangeDays));
        }

        return results;
    }

    private List<PrometheusQueryResponseResult> queryRange(RestClient restClient, String queryString, int rangeDays) {
        PrometheusQueryResponse response;
        try {
            MultiValueMap<String, String> query = PrometheusHelper.queryParameters(queryString, rangeDays);
            response = restClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(query)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PrometheusQueryResponse.class);
        } catch (Exception e) {
            log.warn("Error in Grafana call", e);
            throw PrometheusException.wrapConnectionException(e);
        }
        PrometheusHelper.validateResponse(response);
        return response.getData().getResult();
    }


}
