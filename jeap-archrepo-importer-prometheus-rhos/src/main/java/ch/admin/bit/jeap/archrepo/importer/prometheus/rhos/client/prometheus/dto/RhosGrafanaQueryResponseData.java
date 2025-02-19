package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RhosGrafanaQueryResponseData {
    private Map<String, RhosGrafanaQueryResult> results;
}
