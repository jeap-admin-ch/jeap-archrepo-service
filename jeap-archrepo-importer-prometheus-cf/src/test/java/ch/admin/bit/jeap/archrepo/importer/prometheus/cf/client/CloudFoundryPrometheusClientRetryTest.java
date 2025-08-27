package ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.cf.CloudFoundryImporterConfiguration;
import ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client.prometheus.CloudFoundryPrometheusProxy;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.PrometheusException;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = CloudFoundryImporterConfiguration.class,
        properties = {
                "prometheus-cf.hosts[0].host=dummy"
        })
@Import(CloudFoundryPrometheusClientRetryTest.TestConfig.class)
class CloudFoundryPrometheusClientRetryTest {
    @MockitoBean
    private CloudFoundryPrometheusProxy cloudFoundryPrometheusProxyMock;

    @Autowired
    private CloudFoundryPrometheusClient cloudFoundryPrometheusClient;

    @TestConfiguration
    static class TestConfig {
        @Bean
        CloudFoundryPrometheusClient prometheusClientForTest(CloudFoundryPrometheusProxy cloudFoundryPrometheusProxy) {
            return new CloudFoundryPrometheusClient(cloudFoundryPrometheusProxy);
        }
    }

    @Test
    void listApps_successAfterRetry() {

        PrometheusQueryResponseResult result = new PrometheusQueryResponseResult(Map.of("app_name", "space"));
        when(cloudFoundryPrometheusProxyMock.queryRange(anyString(), anyInt()))
                // First two call fail
                .thenThrow(PrometheusException.class)
                .thenThrow(PrometheusException.class)
                // Third call succeeds
                .thenReturn(List.of(result));

        Set<String> organizations = cloudFoundryPrometheusClient.listApps("myOrg", "ref");

        assertThat(organizations).contains("space");

        verify(cloudFoundryPrometheusProxyMock, times(3)).queryRange(anyString(), anyInt());
    }

}
