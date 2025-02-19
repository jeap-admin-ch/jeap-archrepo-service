package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto;

import lombok.Data;

@Data
public class RhosGrafanaFrame {
    private RhosGrafanaSchema schema;
    private RhosGrafanaData data;
}
