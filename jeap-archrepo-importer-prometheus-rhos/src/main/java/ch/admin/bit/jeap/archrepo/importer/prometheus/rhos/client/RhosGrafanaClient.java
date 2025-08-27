package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.JeapRelation;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.RhosGrafanaAccess;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Retryable(retryFor = Exception.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 2000, multiplier = 2))
@Component
@Slf4j
@RequiredArgsConstructor
public class RhosGrafanaClient {

    private static final String JEAP_RELATION_TOTAL_QUERY = "jeap_relation_total";

    private static final String JEAP_SPRING_APP_BY_NAMESPACE_QUERY = "group by (namespace) (jeap_spring_app)";

    private static final String PROMETHEUS_JEAP_SPRING_APP_BY_SERVICE_NAME_TEMPLATE = "group by(name) (jeap_spring_app{namespace=\"%s\"})";

    /**
     * Import relations from a sliding window of 4 days back until now
     */
    private static final int DAYS_TO_IMPORT = 4;

    private final RhosGrafanaAccess rhosGrafanaAccess;

    public Collection<JeapRelation> apiRelations(String environment) {
        final String stageName = getStageName(environment);
        final List<JeapRelation> result = new ArrayList<>();

        executeQuery(JEAP_RELATION_TOTAL_QUERY, stageName, labels -> {
            PrometheusQueryResponseResult prometheusQueryResponseResult = new PrometheusQueryResponseResult(labels);
            result.add(JeapRelation.fromPrometheusQueryResponseResult(prometheusQueryResponseResult));
        });
        log.info("Found {} relations for stage {}", result.size(), stageName);
        return new TreeSet<>(result);
    }

    public Set<String> namespaces(String environment) {
        final String stageName = getStageName(environment);
        final Set<String> result = new HashSet<>();
        executeQuery(JEAP_SPRING_APP_BY_NAMESPACE_QUERY, stageName,labels -> result.add(labels.get("namespace")));
        log.info("Found namespaces {} for stage {}", result, stageName);
        return result;
    }

    public Set<String> services(String environment, String namespace) {
        final String stageName = getStageName(environment);
        final Set<String> result = new HashSet<>();
        final String query = PROMETHEUS_JEAP_SPRING_APP_BY_SERVICE_NAME_TEMPLATE.formatted(namespace);
        executeQuery(query, stageName,labels -> result.add(labels.get("name")));
        log.info("Found {} services for stage {} and namespace {}", result.size(), stageName, namespace);
        return result;
    }


    private void executeQuery(String query, String stageName, Consumer<Map<String, String>> labelConsumer) {
        final List<RhosGrafanaQueryResponseData> dataResults = rhosGrafanaAccess.queryRange(query, stageName, DAYS_TO_IMPORT);

        for (RhosGrafanaQueryResponseData data : dataResults) {
            Map<String, RhosGrafanaQueryResult> queryResults = data.getResults();
            for (RhosGrafanaQueryResult queryResult : queryResults.values()) {
                if (queryResult.getStatus() == 200) {
                    List<RhosGrafanaFrame> frames = queryResult.getFrames();
                    for (RhosGrafanaFrame frame : frames) {
                        RhosGrafanaSchema schema = frame.getSchema();
                        Optional<RhosGrafanaField> metricsFieldOptional = schema.getMetricsField();
                        if (metricsFieldOptional.isPresent()) {
                            RhosGrafanaField metricsField = metricsFieldOptional.get();
                            Map<String, String> labels = metricsField.getLabels();
                            labelConsumer.accept(labels);
                        }
                    }
                } else {
                    log.info("Got result with status {}, ignoring it", queryResult.getStatus());
                }
            }
        }
    }

    private static String getStageName(String environment) {
        if ("prod".equalsIgnoreCase(environment)) {
            return "p";
        } else if ("abn".equalsIgnoreCase(environment)) {
            return "a";
        }
        return "r";
    }

}
