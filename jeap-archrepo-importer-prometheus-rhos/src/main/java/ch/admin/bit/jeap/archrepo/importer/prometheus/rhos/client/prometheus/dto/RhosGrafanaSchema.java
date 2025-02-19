package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Data
@Slf4j
public class RhosGrafanaSchema {
    private String refId;
    private RhosGrafanaMeta meta;
    private List<RhosGrafanaField> fields;


    public Optional<RhosGrafanaField> getMetricsField() {
        if (fields.size() != 2) {
            log.info("field size is not 2: {}", this);
            return Optional.empty();
        }
        RhosGrafanaField timeField = fields.get(0);
        if (!timeField.isTimeField()) {
            log.info("first field is not time field: {}", this);
            return Optional.empty();
        }
        return Optional.of(fields.get(1));
    }
}