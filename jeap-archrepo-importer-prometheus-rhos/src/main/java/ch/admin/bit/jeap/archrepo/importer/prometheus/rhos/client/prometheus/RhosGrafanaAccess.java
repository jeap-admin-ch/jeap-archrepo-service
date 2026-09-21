package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.PrometheusException;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto.RhosDatasource;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto.RhosGrafanaQueryResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(rhosConnectorProperties.getTimeout());
            requestFactory.setReadTimeout(rhosConnectorProperties.getTimeout());
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
            for (RhosDatasource datasource : queryDatasources(restClient)) {
                if (isApplicationDatasourceForStage(datasource, stageName)) {
                    queryRangeIfApplicable(restClient, datasource, queryExpression, rangeDays)
                            .ifPresent(results::add);
                }
            }
        }

        return results;
    }

    private static boolean isApplicationDatasourceForStage(RhosDatasource datasource, String stageName) {
        return datasource.getName().contains("application") && datasource.getName().endsWith("-" + stageName);
    }

    private Optional<RhosGrafanaQueryResponseData> queryRangeIfApplicable(RestClient restClient, RhosDatasource datasource, String queryExpression, int rangeDays) {
        try {
            return Optional.of(queryRange(restClient, datasource, queryExpression, rangeDays));
        } catch (PrometheusException e) {
            if (isConflictingNamespaceMatcher(e)) {
                // The datasource is bound (via prom-label-proxy) to a namespace that differs from the
                // one referenced in the query. This datasource simply does not apply to this query,
                // so we skip it instead of aborting the whole call and failing all other datasources.
                log.debug("Datasource '{}' is not scoped for query '{}', skipping it: {}",
                        datasource.getName(), queryExpression, e.getMessage());
                return Optional.empty();
            }
            log.warn("Error in Grafana call for datasource '{}'", datasource.getName(), e);
            throw e;
        }
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
            throw PrometheusException.wrapConnectionException(e);
        }
        return response;
    }

    /**
     * Datasources are scoped to a specific namespace by prom-label-proxy, which injects a namespace label matcher
     * into every query and rejects requests whose query already contains a conflicting namespace matcher with a
     * 400 Bad Request. Since we query every "application" datasource of a stage without knowing upfront which
     * namespace(s) it is scoped to, such a conflict is expected for the (many) datasources that are not scoped to
     * the namespace referenced in the query, and must not be treated as a hard failure.
     */
    private static boolean isConflictingNamespaceMatcher(PrometheusException e) {
        return e.getCause() instanceof HttpClientErrorException.BadRequest badRequest
                && badRequest.getResponseBodyAsString().contains("conflicting label matcher");
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
