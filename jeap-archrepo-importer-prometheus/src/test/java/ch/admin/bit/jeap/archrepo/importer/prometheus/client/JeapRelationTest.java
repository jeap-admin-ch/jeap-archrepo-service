package ch.admin.bit.jeap.archrepo.importer.prometheus.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JeapRelationTest {

    @ParameterizedTest
    @CsvSource({"/foo, /foo",
            "/foo/, /foo",
            "/, /"})
    void fromPrometheusQueryResponseResult(String prometheusPath, String datapoint) {
        PrometheusQueryResponseResult result = createPrometheusResult(prometheusPath);

        JeapRelation relation = JeapRelation.fromPrometheusQueryResponseResult(result);

        assertEquals(datapoint, relation.getDatapoint());
    }

    private static PrometheusQueryResponseResult createPrometheusResult(String v1) {
        Map<String, String> metrics = Map.of(
                "datapoint", v1
        );
        return new PrometheusQueryResponseResult(metrics);
    }
}
