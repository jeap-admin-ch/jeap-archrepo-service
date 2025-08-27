package ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.aws.AWSImporterConfiguration;
import ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client.prometheus.AWSPrometheusProxy;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.PrometheusException;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = AWSImporterConfiguration.class,
        properties = {"prometheus-aws.host=https://localhost"})
@Import(AWSPrometheusClientRetryTest.TestConfig.class)
class AWSPrometheusClientRetryTest {
    @MockBean
    private AWSPrometheusProxy awsPrometheusProxy;

    @Autowired
    private AWSPrometheusClient awsPrometheusClient;

    @TestConfiguration
    static class TestConfig {
        @Bean
        AWSPrometheusClient awsPrometheusClient(AWSPrometheusProxy awsPrometheusProxy) {
            return new AWSPrometheusClient(awsPrometheusProxy);
        }
    }

    @Test
    void listApplications_successAfterRetry() {

        PrometheusQueryResponseResult result = new PrometheusQueryResponseResult(Map.of("application", "test"));
        when(awsPrometheusProxy.queryRange(anyString(), anyInt()))
                // First two call fail
                .thenThrow(PrometheusException.class)
                .thenThrow(PrometheusException.class)
                // Third call succeeds
                .thenReturn(List.of(result));

        Set<String> organizations = awsPrometheusClient.listApplications("myOrg");

        assertThat(organizations).contains("test");

        verify(awsPrometheusProxy, times(3)).queryRange(anyString(), anyInt());
    }

}
