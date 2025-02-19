package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RhosGrafanaSchemaTest {

    @Test
    void getMetricsFieldReturnMetricsFieldWhenAllNormal() {
        RhosGrafanaSchema schema = new RhosGrafanaSchema();
        RhosGrafanaField timeField = new RhosGrafanaField();
        timeField.setName("Time");
        RhosGrafanaField metricsField = new RhosGrafanaField();
        metricsField.setName("jeap_spring_app");
        metricsField.setLabels(Map.of("namespace", "bit-jme-d", "dummy", "dummy"));
        schema.setFields(List.of(timeField, metricsField));

        Optional<RhosGrafanaField> metricsFieldOptional = schema.getMetricsField();
        assertEquals(metricsField, metricsFieldOptional.get());
    }

    @Test
    void getMetricsFieldReturnEmptyOptionalWhenNotTwoFields() {
        RhosGrafanaSchema schema = new RhosGrafanaSchema();
        RhosGrafanaField metricsField = new RhosGrafanaField();
        metricsField.setLabels(Map.of("namespace", "bit-jme-d", "dummy", "dummy"));
        schema.setFields(List.of(metricsField));

        assertFalse(schema.getMetricsField().isPresent());
    }

    @Test
    void getMetricsFieldReturnEmptyOptionalWhenNoTimeField() {
        RhosGrafanaSchema schema = new RhosGrafanaSchema();
        RhosGrafanaField otherField = new RhosGrafanaField();
        otherField.setName("Dummy");
        RhosGrafanaField metricsField = new RhosGrafanaField();
        metricsField.setName("jeap_spring_app");
        metricsField.setLabels(Map.of("namespace", "bit-jme-d", "dummy", "dummy"));
        schema.setFields(List.of(otherField, metricsField));

        assertFalse(schema.getMetricsField().isPresent());
    }
}