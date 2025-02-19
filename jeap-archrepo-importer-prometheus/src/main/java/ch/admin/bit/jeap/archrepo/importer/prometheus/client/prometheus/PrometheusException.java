package ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus;

import java.net.URISyntaxException;

public class PrometheusException extends RuntimeException {
    private PrometheusException(String message, Throwable cause) {
        super(message, cause);
    }

    private PrometheusException(String message) {
        super(message);
    }

    public static PrometheusException wrapConnectionException(Exception e) {
        return new PrometheusException("Could not connect to Grafana", e);
    }

    public static PrometheusException uriSyntaxException(URISyntaxException e) {
        return new PrometheusException("Could not create uri to Grafana", e);
    }

    public static PrometheusException responseNotSuccessful() {
        return new PrometheusException("Prometheus returned unsuccessful result");
    }

    public static PrometheusException responseNotMatrix() {
        return new PrometheusException("Prometheus did not return a matrix result even when it was anticipated");
    }
}
