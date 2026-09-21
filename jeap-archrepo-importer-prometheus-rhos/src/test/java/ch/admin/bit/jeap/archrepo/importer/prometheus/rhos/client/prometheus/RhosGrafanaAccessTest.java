package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.PrometheusException;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto.RhosGrafanaQueryResponseData;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that {@link RhosGrafanaAccess} correctly handles Grafana datasources that are scoped (via prom-label-proxy)
 * to a namespace other than the one referenced in the query: such datasources must be skipped instead of aborting
 * the whole {@link RhosGrafanaAccess#queryRange(String, String, int)} call, since we do not know upfront which
 * namespace(s) a datasource is scoped to.
 */
class RhosGrafanaAccessTest {

    private static final String CONFLICTING_NAMESPACE_MATCHER_BODY = """
            {"results":{"A":{"error":"prom-label-proxy: conflicting label matcher: label matcher \\"namespace=\\\\\\"sgc-kop-d\\\\\\"\\" conflicts with injected matcher \\"namespace=\\\\\\"sgc-portale-d\\\\\\"\\"","status":400}}}""";

    private static final String OTHER_ERROR_BODY = """
            {"message":"unauthorized"}""";

    private static final String SUCCESS_BODY = """
            {"results":{"A":{"status":200,"frames":[]}}}""";

    private HttpServer server;
    private String datasourceOkUid;
    private String datasourceConflictingUid;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void queryRange_oneDatasourceScopedToOtherNamespace_skipsItAndReturnsResultsFromMatchingDatasource() {
        datasourceOkUid = java.util.UUID.randomUUID().toString();
        datasourceConflictingUid = java.util.UUID.randomUUID().toString();
        server.createContext("/api/datasources", datasourcesHandler(datasourceOkUid, datasourceConflictingUid));
        server.createContext("/api/ds/query", queryHandler(uid -> {
            if (uid.equals(datasourceConflictingUid)) {
                return new Response(400, CONFLICTING_NAMESPACE_MATCHER_BODY);
            }
            return new Response(200, SUCCESS_BODY);
        }));

        RhosGrafanaAccess grafanaAccess = createGrafanaAccess();

        List<RhosGrafanaQueryResponseData> results = grafanaAccess.queryRange("group by(name) (jeap_spring_app{namespace=\"my-namespace-d\"})", "d", 4);

        assertEquals(1, results.size());
    }

    @Test
    void queryRange_allDatasourcesScopedToOtherNamespace_returnsEmptyList() {
        datasourceConflictingUid = java.util.UUID.randomUUID().toString();
        server.createContext("/api/datasources", datasourcesHandler(datasourceConflictingUid));
        server.createContext("/api/ds/query", queryHandler(uid -> new Response(400, CONFLICTING_NAMESPACE_MATCHER_BODY)));

        RhosGrafanaAccess grafanaAccess = createGrafanaAccess();

        List<RhosGrafanaQueryResponseData> results = grafanaAccess.queryRange("group by(name) (jeap_spring_app{namespace=\"my-namespace-d\"})", "d", 4);

        assertEquals(0, results.size());
    }

    @Test
    void queryRange_unrelatedBadRequest_isNotSwallowedAndThrows() {
        datasourceOkUid = java.util.UUID.randomUUID().toString();
        server.createContext("/api/datasources", datasourcesHandler(datasourceOkUid));
        server.createContext("/api/ds/query", queryHandler(uid -> new Response(400, OTHER_ERROR_BODY)));

        RhosGrafanaAccess grafanaAccess = createGrafanaAccess();

        assertThrows(PrometheusException.class,
                () -> grafanaAccess.queryRange("up", "d", 4));
    }

    private RhosGrafanaAccess createGrafanaAccess() {
        RhosConnectorProperties properties = new RhosConnectorProperties();
        properties.setHosts(List.of(new RhosHostProperties("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort(), "dummy-token")));
        return new RhosGrafanaAccess(properties, RestClient.builder());
    }

    private com.sun.net.httpserver.HttpHandler datasourcesHandler(String... uids) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < uids.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("{\"id\":").append(i + 1)
                    .append(",\"uid\":\"").append(uids[i]).append("\"")
                    .append(",\"name\":\"application-").append(i).append("-d\"}");
        }
        json.append("]");
        String body = json.toString();
        return exchange -> writeResponse(exchange, 200, body);
    }

    private com.sun.net.httpserver.HttpHandler queryHandler(java.util.function.Function<String, Response> responseForUid) {
        AtomicInteger callCount = new AtomicInteger();
        return exchange -> {
            callCount.incrementAndGet();
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String uid = extractUid(requestBody);
            Response response = responseForUid.apply(uid);
            writeResponse(exchange, response.status(), response.body());
        };
    }

    private static String extractUid(String requestBody) {
        int idx = requestBody.indexOf("\"uid\": \"");
        int start = idx + "\"uid\": \"".length();
        int end = requestBody.indexOf('"', start);
        return requestBody.substring(start, end);
    }

    private static void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private record Response(int status, String body) {
    }
}
