package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto;

import lombok.Data;

import java.util.List;

@Data
public class RhosGrafanaMeta {
    private String type;
    private List<Integer> typeVersion;
    private RhosGrafanaCustom custom;
    private String executedQueryString;
}
