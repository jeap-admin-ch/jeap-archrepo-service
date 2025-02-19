package ch.admin.bit.jeap.archrepo.importer.prometheus.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import lombok.Value;

import java.util.Map;

@Value
public class JeapRelation implements Comparable<JeapRelation> {

    String provider;

    String consumer;

    String method;

    String technology;

    String datapoint;

    public static JeapRelation fromPrometheusQueryResponseResult(PrometheusQueryResponseResult result) {
        final Map<String, String> metric = result.getMetric();
        String datapoint = datapointWithoutTrailingSlash(metric);
        return new JeapRelation(metric.get("producer"), metric.get("consumer"), metric.get("method"), metric.get("technology"), datapoint);
    }

    private static String datapointWithoutTrailingSlash(Map<String, String> metric) {
        String datapoint = metric.get("datapoint");
        if (datapoint == null || !datapoint.endsWith("/") || datapoint.length() == 1) {
            return datapoint;
        }
        return datapoint.substring(0, datapoint.length() - 1);
    }

    @Override
    public int compareTo(JeapRelation jeapRelation) {
        return compare(provider, jeapRelation.provider) * 10000 +
                compare(consumer, jeapRelation.consumer) * 1000 +
                compare(method, jeapRelation.method) * 100 +
                compare(technology, jeapRelation.technology) * 10 +
                compare(datapoint, jeapRelation.datapoint);
    }

    private int compare(String a, String b) {
        if (a == null && b == null) {
            return 0;
        } else if (a == null) {
            return 1;
        } else if (b == null) {
            return -1;
        }
        return a.compareTo(b);
    }
}
