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

    private static final List<String> STAGES = List.of("r", "a", "p");

    private static final String JEAP_RELATION_TOTAL_QUERY = "jeap_relation_total";

    private static final String JEAP_SPRING_APP_BY_NAMESPACE_QUERY = "group by (namespace) (jeap_spring_app)";

    private static final String PROMETHEUS_JEAP_SPRING_APP_BY_SERVICE_NAME_TEMPLATE = "group by(name) (jeap_spring_app{namespace=\\\"%s\\\"})";

    /**
     * Import relations from a sliding window of 4 days back until now
     */
    private static final int DAYS_TO_IMPORT = 4;

    private final RhosGrafanaAccess rhosGrafanaAccess;

    public Collection<JeapRelation> apiRelations() {
        return STAGES.stream().flatMap(this::apiRelationsForStage)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Set<NamespaceStagePair> namespaces(Set<String> stageNames) {
        return stageNames.stream()
                .flatMap(stageName -> namespaces(stageName))
                .collect(Collectors.toSet());
    }

    public Set<String> services(NamespaceStagePair namespaceStagePair) {
        final Set<String> result = new HashSet<>();
        final String query = PROMETHEUS_JEAP_SPRING_APP_BY_SERVICE_NAME_TEMPLATE.formatted(namespaceStagePair.getNamespaceName());

        executeQuery(query, namespaceStagePair.getStageName(), DAYS_TO_IMPORT,
                labels -> result.add(labels.get("name")));
        log.info("Found {} relations for namespace {}", result.size(), namespaceStagePair.getNamespaceName());

        return result;
    }

    private Stream<JeapRelation> apiRelationsForStage(String stageName) {
        final List<JeapRelation> result = new ArrayList<>();

        executeQuery(JEAP_RELATION_TOTAL_QUERY, stageName, DAYS_TO_IMPORT, labels -> {
            PrometheusQueryResponseResult prometheusQueryResponseResult = new PrometheusQueryResponseResult(labels);
            result.add(JeapRelation.fromPrometheusQueryResponseResult(prometheusQueryResponseResult));
        });
        log.info("Found {} relations for stage {}", result.size(), stageName);

        return result.stream();
    }

    private Stream<NamespaceStagePair> namespaces(String stageName) {
        final Set<NamespaceStagePair> result = new HashSet<>();

        executeQuery(JEAP_SPRING_APP_BY_NAMESPACE_QUERY, stageName, DAYS_TO_IMPORT,
                labels -> result.add(new NamespaceStagePair(labels.get("namespace"), stageName))
        );

        return result.stream();
    }

    private void executeQuery(String query, String stageName, int rangeDays, Consumer<Map<String, String>> labelConsumer) {
        final List<RhosGrafanaQueryResponseData> dataResults = rhosGrafanaAccess.queryRange(query, stageName, rangeDays);

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


}