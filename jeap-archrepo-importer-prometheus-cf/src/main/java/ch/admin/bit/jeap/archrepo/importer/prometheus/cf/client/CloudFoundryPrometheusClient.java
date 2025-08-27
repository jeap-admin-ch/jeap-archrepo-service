package ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client.prometheus.CloudFoundryPrometheusProxy;
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
public class CloudFoundryPrometheusClient {

    private static final String PROMETHEUS_QUERY_TEMPLATE = "jeap_relation_total{space_name=\"%s\"}";

    private static final String PROMETHEUS_JEAP_SPRING_APP_ORG_NAME_TEMPLATE = """
            group by(org_name) (jeap_spring_app{space_name="%s"})
            """;

    private static final String PROMETHEUS_JEAP_SPRING_APP_APPS_TEMPLATE = """
            group by(app_name) (jeap_spring_app{org_name="%s", space_name="%s"})
            """;
    private static final String CF_APP_NAME_LABEL = "app_name";

    private static final String CF_ORG_NAME_LABEL = "org_name";

    /**
     * Import relations from a sliding window of 4 days back until now
     */
    private static final int DAYS_TO_IMPORT = 4;

    private final CloudFoundryPrometheusProxy cloudFoundryPrometheusProxy;

    public Collection<JeapRelation> apiRelations(String space) {
        return apiRelationsForSpace(space).collect(Collectors.toCollection(TreeSet::new));
    }

    private Stream<JeapRelation> apiRelationsForSpace(String space) {
        String query = PROMETHEUS_QUERY_TEMPLATE.formatted(space);
        final List<PrometheusQueryResponseResult> results = cloudFoundryPrometheusProxy.queryRange(query, DAYS_TO_IMPORT);
        log.info("Found {} relations for space {}", results.size(), space);
        return results.stream()
                .map(JeapRelation::fromPrometheusQueryResponseResult);
    }

    public Set<String> listOrganisationWithMatchingPrefix(Collection<String> prefixes, String space) {
        final List<PrometheusQueryResponseResult> results = cloudFoundryPrometheusProxy.queryRange(PROMETHEUS_JEAP_SPRING_APP_ORG_NAME_TEMPLATE.formatted(space), DAYS_TO_IMPORT);
        log.info("Found {} organisations in space {}", results.size(), space);

        return results.stream()
                .map(metric -> metric.getMetric().get(CF_ORG_NAME_LABEL))
                .filter(org -> prefixMatch(org, prefixes))
                .collect(Collectors.toSet());
    }

    private boolean prefixMatch(String org, Collection<String> prefixes) {
        return prefixes.stream().anyMatch(prefix -> org != null && org.startsWith(prefix));
    }

    public Set<String> listApps(String org, String space) {
        String query = PROMETHEUS_JEAP_SPRING_APP_APPS_TEMPLATE.formatted(org, space);
        final List<PrometheusQueryResponseResult> results = cloudFoundryPrometheusProxy.queryRange(query, DAYS_TO_IMPORT);
        log.info("Found {} apps for org {} in space {}", results.size(), org, space);
        return results.stream().map(metric -> metric.getMetric().get(CF_APP_NAME_LABEL)).collect(Collectors.toSet());
    }
}
