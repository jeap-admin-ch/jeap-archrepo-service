package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "prometheus-rhos", ignoreUnknownFields = false)
@Slf4j
public class RhosConnectorProperties {
    /**
     * List of Grafana hosts
     */
    private List<RhosHostProperties> hosts;

    /**
     * Timeout when connecting to grafana
     */
    private Duration timeout = Duration.ofSeconds(120);


    @PostConstruct
    void init() {
        log.info("GrafanaConnector configuration: {}", this);
    }
}
