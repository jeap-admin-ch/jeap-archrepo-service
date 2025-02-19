package ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto;

import lombok.Value;

import java.util.List;

@Value
public class PrometheusQueryResponseData {
    String resultType;
    List<PrometheusQueryResponseResult> result;
}
