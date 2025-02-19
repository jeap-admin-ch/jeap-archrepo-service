package ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client.prometheus;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "prometheus-cf", ignoreUnknownFields = false)
@Slf4j
public class CloudFoundryConnectorProperties {
    /**
     * List of Grafana hosts
     */
    private List<CloudFoundryHostProperties> hosts;
    /**
     * Id of the prometheus datasource in grafana
     */
    private String datasource = "1";
    /**
     * Timeout when connecting to grafana
     */
    private Duration timeout = Duration.ofSeconds(120);
    /**
     * Max memory size for grafana responses. They can become quite big if you perform
     * queries with few filter
     */
    private int maxInMemorySize = 16 * 1024 * 1024;

    @PostConstruct
    void init() {
        log.info("GrafanaConnector configuration: {}", this);
    }
}