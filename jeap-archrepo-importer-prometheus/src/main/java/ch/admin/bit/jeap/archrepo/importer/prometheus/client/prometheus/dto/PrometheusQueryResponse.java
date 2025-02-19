package ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto;

import lombok.Value;

@Value
public class PrometheusQueryResponse {
    String status;
    PrometheusQueryResponseData data;
}
