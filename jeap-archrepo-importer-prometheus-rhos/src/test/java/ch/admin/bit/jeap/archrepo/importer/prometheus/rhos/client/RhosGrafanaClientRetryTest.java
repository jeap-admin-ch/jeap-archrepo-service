package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.PrometheusException;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.RhosImporterConfiguration;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.RhosGrafanaAccess;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = RhosImporterConfiguration.class,
        properties = {
                "prometheus-rhos.hosts[0].host=dummy"
        })
@Import(RhosGrafanaClientRetryTest.TestConfig.class)
class RhosGrafanaClientRetryTest {

    @MockBean
    private RhosGrafanaAccess grafanaAccess;

    @Autowired
    private RhosGrafanaClient grafanaClient;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        RhosGrafanaClient grafanaClientForTest(RhosGrafanaAccess grafanaAccess) {
            return new RhosGrafanaClient(grafanaAccess);
        }
    }

    @Test
    void namespaces_successAfterRetry() {
        final String namespaceName = "bit-mysystem-d";
        RhosGrafanaQueryResponseData responseData = prepareResponseData(namespaceName);

        when(grafanaAccess.queryRange(anyString(), anyString(), anyInt()))
                // First two call fail
                .thenThrow(PrometheusException.class)
                .thenThrow(PrometheusException.class)
                // Third call succeeds
                .thenReturn(List.of(responseData));

        Set<NamespaceStagePair> namespaceStagePairs = grafanaClient.namespaces(Set.of("d"));

        assertEquals(1, namespaceStagePairs.size());
        NamespaceStagePair namespaceStagePair = namespaceStagePairs.iterator().next();
        assertEquals(namespaceName, namespaceStagePair.getNamespaceName());
        assertEquals("d", namespaceStagePair.getStageName());

        verify(grafanaAccess, times(3)).queryRange(anyString(), anyString(), anyInt());
    }

    private static RhosGrafanaQueryResponseData prepareResponseData(String namespaceName) {
        RhosGrafanaSchema schema = new RhosGrafanaSchema();
        RhosGrafanaField timeField = new RhosGrafanaField();
        timeField.setName("Time");
        RhosGrafanaField metricField = new RhosGrafanaField();
        metricField.setName("jeap_spring_app");
        metricField.setLabels(Map.of("namespace", namespaceName, "dummy", "dummy"));
        schema.setFields(List.of(timeField, metricField));
        RhosGrafanaFrame frame = new RhosGrafanaFrame();

        frame.setSchema(schema);
        RhosGrafanaQueryResult result = new RhosGrafanaQueryResult();
        result.setStatus(200);

        result.setFrames(List.of(frame));

        RhosGrafanaQueryResponseData responseData = new RhosGrafanaQueryResponseData();
        responseData.setResults(Map.of("A", result));
        return responseData;
    }
}