package ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.util.Map;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrometheusQueryResponseResult {
    Map<String, String> metric;
}
