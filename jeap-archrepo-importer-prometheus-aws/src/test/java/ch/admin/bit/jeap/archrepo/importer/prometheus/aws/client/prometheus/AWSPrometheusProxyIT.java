package ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "up",
            "jeap_relation_total{stage=\"ref\"}",
            "group by(account_id) (jeap_spring_app)",
            "jeap_spring_app{stage=\"ref\", account_id=\"58264373351\"}"
    })
    void queryRangeReturnsResults(String query) {
        List<PrometheusQueryResponseResult> results = awsPrometheusProxy.queryRange(query, 4);

        log.info("query '{}' returned {} results", query, results.size());

        assertTrue(results.size() > 4, "There must be many results");
    }
}
