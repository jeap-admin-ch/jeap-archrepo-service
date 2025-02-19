package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto.RhosGrafanaQueryResponseData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Some integration tests to check the connection to grafana. Those tests are disable by default as they need
 * a grafana service account token which we do not want to check in to git. You can run them manually if you provide the password.
 */
@Slf4j
@Disabled
class RhosGrafanaAccessIT {

    private RhosGrafanaAccess grafanaAccess;

    @BeforeEach
    void setup() {
        RhosConnectorProperties properties = new RhosConnectorProperties();
        properties.setHosts(List.of(new RhosHostProperties("https://<<yourUrlToGrafana>>", "serviceAccountToken")));
        grafanaAccess = new RhosGrafanaAccess(properties, RestClient.builder());
    }

    @Test
    void up() {
        List<RhosGrafanaQueryResponseData> results = grafanaAccess.queryRange("up", "d", 4);
        log.info("results {}", results);
        assertFalse(results.isEmpty(), "There must be at least one result");
    }

}