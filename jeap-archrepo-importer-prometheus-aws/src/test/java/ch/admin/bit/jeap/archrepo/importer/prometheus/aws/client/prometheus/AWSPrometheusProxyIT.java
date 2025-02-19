package ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@Disabled
class AWSPrometheusProxyIT {

    private static AWSPrometheusProxy awsPrometheusProxy;

    @BeforeAll
    static void setup() {
        AWSConnectorProperties properties = new AWSConnectorProperties();
        properties.setHost("https://aps-workspaces.eu-central-1.amazonaws.com");
        properties.setWorkspace("ws-419f038a-efcf-4981-9945-f34a0ad35032b2");
        properties.setRoleArn("arn:aws:iam::891377051866:role/nivel-amp-cross-account-read-assume-role");
        properties.setRoleSessionName("mySession");

        awsPrometheusProxy = new AWSPrometheusProxy(properties);
    }

    @Test
    void up() {
        List<PrometheusQueryResponseResult> results = awsPrometheusProxy.queryRange("up", 4);
        log.info("results {}", results);
        assertTrue(results.size() > 4, "There must be many results");
    }

    @Test
    void queryRange() {
        List<PrometheusQueryResponseResult> results = awsPrometheusProxy.queryRange("jeap_relation_total{stage=\"ref\"}", 4);

        log.info("results {}", results);

        assertTrue(results.size() > 4, "There must be many results");
    }

    @Test
    void queryAppOrgName(){

        String query = """
            group by(account_id) (jeap_spring_app)
            """;

        final List<PrometheusQueryResponseResult> results = awsPrometheusProxy.queryRange(query, 4);
        log.info("Found {} organisations", results.size());

        assertTrue(results.size() > 4, "There must be many results");
    }

    @Test
    void querySpringApp() {
        List<PrometheusQueryResponseResult> results = awsPrometheusProxy.queryRange("jeap_spring_app{stage=\"ref\", account_id=\"58264373351\"}", 4);

        log.info("results {}", results);

        assertTrue(results.size() > 4, "There must be many results");
    }
}
