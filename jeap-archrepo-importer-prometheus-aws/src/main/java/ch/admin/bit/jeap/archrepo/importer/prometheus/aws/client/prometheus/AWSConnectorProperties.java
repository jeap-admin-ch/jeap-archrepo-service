package ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client.prometheus;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "prometheus-aws", ignoreUnknownFields = false)
@Slf4j
public class AWSConnectorProperties {

    private String host;

    private String roleArn;

    private String roleSessionName;

    private String workspace;
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
        log.info("AWSConnector configuration: {}", this);
    }
}
