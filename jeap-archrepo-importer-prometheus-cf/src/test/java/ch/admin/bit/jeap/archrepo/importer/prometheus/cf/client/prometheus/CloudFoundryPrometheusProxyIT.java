package ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Some integration tests to check the connection to grafana. Those tests are disable by default as they need
 * a grafana api-key which we do not want to check in to git. You can run them manually if you provide the password.
 */
@Slf4j
@Disabled
class CloudFoundryPrometheusProxyIT {
    private static CloudFoundryPrometheusProxy cloudFoundryPrometheusProxy;

    @BeforeAll
    static void setup() {
        CloudFoundryConnectorProperties properties = new CloudFoundryConnectorProperties();
        properties.setHosts(List.of(new CloudFoundryHostProperties("https://<<yourGrafanUrl>>", "apikey"),
                new CloudFoundryHostProperties("https://<<yourGrafanUrl>>", "apikey"),
                new CloudFoundryHostProperties("https://<<yourGrafanUrl>>", "apikey")));
        cloudFoundryPrometheusProxy = new CloudFoundryPrometheusProxy(properties, RestClient.builder());
    }

    @Test
    void up() {
        List<PrometheusQueryResponseResult> results = cloudFoundryPrometheusProxy.queryRange("up", 4);
        log.info("results {}", results);
        assertTrue(results.size() > 4, "There must be many results");
    }

    @Test
    void queryRange() {
        List<PrometheusQueryResponseResult> results = cloudFoundryPrometheusProxy.queryRange("jeap_relation_total{space_name=\"ref\"}", 4);

        log.info("results {}", results);

        assertTrue(results.size() > 4, "There must be many results");
    }

    @Test
    void querySpringApp() {
        List<PrometheusQueryResponseResult> results = cloudFoundryPrometheusProxy.queryRange("jeap_spring_app{space_name=\"ref\", org_name=\"BAZG-Activ\"}", 4);

        log.info("results {}", results);

        assertTrue(results.size() > 4, "There must be many results");
    }
}
