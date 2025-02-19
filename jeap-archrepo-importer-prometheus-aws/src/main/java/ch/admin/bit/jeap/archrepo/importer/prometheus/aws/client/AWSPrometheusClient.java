package ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client.prometheus.AWSPrometheusProxy;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.JeapRelation;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Retryable(retryFor = Exception.class,
        maxAttempts = 4,
        backoff = @Backoff(delay = 2000, multiplier = 2))
@Component
@Slf4j
@RequiredArgsConstructor
public class AWSPrometheusClient {

    private static final List<String> STAGES = List.of("ref", "abn", "prod");
    private static final String PROMETHEUS_QUERY_TEMPLATE = "jeap_relation_total{stage=\"%s\"}";

    private static final String PROMETHEUS_QUERY_APP_TEMPLATE = "group by (application) (jeap_spring_app)";

    private static final String PROMETHEUS_JEAP_SPRING_APP_STAGE_TEMPLATE = """
            group by(stage) (jeap_spring_app{application="%s"})
            """;

    private static final String PROMETHEUS_JEAP_SPRING_APP_SERVICES_TEMPLATE = """
            group by(service) (jeap_spring_app{application="%s", stage="%s"})
            """;

    /**
     * Import relations from a sliding window of 4 days back until now
     */
    private static final int DAYS_TO_IMPORT = 4;

    private final AWSPrometheusProxy prometheusProxy;

    public Collection<JeapRelation> apiRelations() {
        return STAGES.stream().flatMap(this::apiRelationsForStage)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Stream<JeapRelation> apiRelationsForStage(String stage) {
        final List<PrometheusQueryResponseResult> results = prometheusProxy.queryRange(PROMETHEUS_QUERY_TEMPLATE.formatted(stage), DAYS_TO_IMPORT);
        log.info("Found {} relations for stage {}", results.size(), stage);
        return results.stream()
                .map(JeapRelation::fromPrometheusQueryResponseResult);
    }

    public Set<String> listApplications() {
        final List<PrometheusQueryResponseResult> results = prometheusProxy.queryRange(PROMETHEUS_QUERY_APP_TEMPLATE, DAYS_TO_IMPORT);
        log.info("Found {} applications", results.size());
        return results.stream()
                .map(metric -> metric.getMetric().get("application"))
                .collect(Collectors.toSet());
    }

    public Set<String> listStages(String application) {
        final List<PrometheusQueryResponseResult> results = prometheusProxy.queryRange(PROMETHEUS_JEAP_SPRING_APP_STAGE_TEMPLATE.formatted(application), DAYS_TO_IMPORT);
        log.info("Found {} stages for application {}", results.size(), application);
        return results.stream().map(metric -> metric.getMetric().get("stage")).collect(Collectors.toSet());
    }

    public Set<String> listServices(String application, String stage) {
        String query = PROMETHEUS_JEAP_SPRING_APP_SERVICES_TEMPLATE.formatted(application, stage);
        final List<PrometheusQueryResponseResult> results = prometheusProxy.queryRange(query, DAYS_TO_IMPORT);
        log.info("Found {} services for application {} in stage {}", results.size(), application, stage);
        return results.stream().map(metric -> metric.getMetric().get("service")).collect(Collectors.toSet());
    }
}
