package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RhosGrafanaField {
    private static final String TIME_FIELD_NAME = "Time";

    private String name;
    private String type;
    private RhosGrafanaTypeInfo typeInfo;
    private RhosGrafanaConfig config;
    private Map<String, String> labels;

    boolean isTimeField() {
        return TIME_FIELD_NAME.equals(name);
    }
}
