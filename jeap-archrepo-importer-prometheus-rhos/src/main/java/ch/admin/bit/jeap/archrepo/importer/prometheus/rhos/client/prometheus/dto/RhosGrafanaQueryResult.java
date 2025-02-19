package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto;

import lombok.Data;

import java.util.List;

@Data
public class RhosGrafanaQueryResult {
    private int status;
    private List<RhosGrafanaFrame> frames;
}
