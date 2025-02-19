package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.PrometheusException;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto.RhosDatasource;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto.RhosGrafanaQueryResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * This class can be used to query prometheus metrics trough grafana. We will therefore use the ds/query API of
 * Grafana that allows us to proxy requests to a specific datasource through grafana.
 * The datasource proxy API doe not work on RHOS Grafana.
 * Therewith we can use the user management features of grafana and the filtering that on prometheus that is already
 * in place and do not rely directly on additional prometheus infrastructure
 */
@Component
@Slf4j
public class RhosGrafanaAccess {
    private static final String API_URL_PATTERN = "%s/api/";
    private static final String RANGE_QUERY_JSON = """
            {
              "queries": [
                {
                  "intervalMs": 60000,
                  "range": true,
                  "instant": false,
                  "datasource": {
                    "uid": "%s"
                  },
                  "expr": "%s"
                }
              ],
              "from": "now-%sd",
              "to": "now"
            }""";

    private final List<RestClient> restClients;


    public RhosGrafanaAccess(RhosConnectorProperties rhosConnectorProperties, RestClient.Builder restClientBuilder) {
        restClients = new ArrayList<>();

        for (RhosHostProperties host : rhosConnectorProperties.getHosts()) {
            log.info("Configuring GrafanaClient for host '{}'", host.getHost());
            String baseUrl = String.format(API_URL_PATTERN, host.getHost());
            ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                    .withReadTimeout(rhosConnectorProperties.getTimeout()));
            restClients.add(restClientBuilder
                    .requestFactory(requestFactory)
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + host.getServiceAccountToken())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build());
        }
    }

    public List<RhosGrafanaQueryResponseData> queryRange(String queryExpression, String stageName, int rangeDays) {
        List<RhosGrafanaQueryResponseData> results = new ArrayList<>();

        for (RestClient restClient : this.restClients) {
            List<RhosDatasource> datasources = queryDatasources(restClient);
            for (RhosDatasource datasource : datasources) {
                if (datasource.getName().contains("application") && datasource.getName().endsWith("-" + stageName)) {
                    RhosGrafanaQueryResponseData rhosPrometheusQueryResponseData = queryRange(restClient, datasource, queryExpression, rangeDays);
                    results.add(rhosPrometheusQueryResponseData);
                }
            }
        }

        return results;
    }

    private RhosGrafanaQueryResponseData queryRange(RestClient restClient, RhosDatasource rhosDatasource, String queryExpression, int rangeDays) {
        RhosGrafanaQueryResponseData response;
        try {
            String rangeQuery = RANGE_QUERY_JSON.formatted(rhosDatasource.getUid(), queryExpression, rangeDays);
            response = restClient.post()
                    .uri("ds/query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rangeQuery)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(RhosGrafanaQueryResponseData.class);
        } catch (Exception e) {
            log.warn("Error in Grafana call", e);
            throw PrometheusException.wrapConnectionException(e);
        }
        //PrometheusHelper.validateResponse(response);
        return response;
    }

    private List<RhosDatasource> queryDatasources(RestClient restClient) {
        List<RhosDatasource> response;
        try {
            response = restClient.get()
                    .uri("datasources")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RhosDatasource>>() {
                    });
        } catch (Exception e) {
            log.warn("Error in Grafana call", e);
            throw PrometheusException.wrapConnectionException(e);
        }
        return response;
    }

}
