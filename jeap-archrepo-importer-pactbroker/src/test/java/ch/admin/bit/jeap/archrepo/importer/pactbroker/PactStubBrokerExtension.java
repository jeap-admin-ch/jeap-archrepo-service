package ch.admin.bit.jeap.archrepo.importer.pactbroker;

import ch.admin.bit.jeap.archrepo.importer.pactbroker.client.PactBrokerClient;
import ch.admin.bit.jeap.archrepo.importer.pactbroker.client.PactBrokerConfig;
import ch.admin.bit.jeap.archrepo.importer.pactbroker.client.PactBrokerProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

public class PactStubBrokerExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, ParameterResolver {
    private static final String APPLICATION_HAL_JSON = "application/hal+json";
    private static final int PORT_NUMBER = 22412;

    private WireMockServer wireMockServer;

    @Override
    public void beforeAll(ExtensionContext context) {
        wireMockServer = new WireMockServer(wireMockConfig().port(PORT_NUMBER));
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        wireMockServer.stop();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_HAL_JSON)
                        .withBody(getClass().getResourceAsStream("/pactbroker-hal-root.json").readAllBytes())));

        stubFor(get(urlPathEqualTo("/pacts/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_HAL_JSON)
                        .withBody(getClass().getResourceAsStream("/latest-pacts-response.json").readAllBytes())));

        stubFor(get(urlPathMatching("/pacts/provider/.*/consumer/.*/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_HAL_JSON)
                        .withBody(getClass().getResourceAsStream("/jme-pact-response.json").readAllBytes())));

        stubFor(get(urlPathMatching("/pacts/provider/ezv-shared-agir-service/consumer/bit-jeap-error-handling-service/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_HAL_JSON)
                        .withBody(getClass().getResourceAsStream("/agir-errorhandling-pact-response.json").readAllBytes())));

        stubFor(get(urlPathMatching("/pacts/provider/bit-jme-cdc-segregatedProvider-service_moduleA/consumer/bit-jme-cdc-consumer-service/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_HAL_JSON)
                        .withBody(getClass().getResourceAsStream("/jme-cdc-segregatedProvider-service_moduleA.json").readAllBytes())));

        stubFor(get(urlPathMatching("/pacts/provider/bit-jme-cdc-segregatedProvider-service_moduleB/consumer/bit-jme-cdc-consumer-service/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_HAL_JSON)
                        .withBody(getClass().getResourceAsStream("/jme-cdc-segregatedProvider-service_moduleB.json").readAllBytes())));

    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType()
                .equals(PactBrokerClient.class);
    }

    @SneakyThrows
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        PactBrokerProperties props = new PactBrokerProperties(wireMockServer.baseUrl(), null, null);
        return new PactBrokerConfig().pactBrokerClient(props);
    }
}
