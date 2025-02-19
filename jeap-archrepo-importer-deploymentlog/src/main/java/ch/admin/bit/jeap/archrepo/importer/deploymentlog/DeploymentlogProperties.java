package ch.admin.bit.jeap.archrepo.importer.deploymentlog;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "deploymentlog", ignoreUnknownFields = false)
@Slf4j
public class DeploymentlogProperties {
    /**
     * URL of deploymentlog
     */
    private String url;
    /**
     * Environment name from which to import components
     */
    private String environment = "ref";
    /**
     * Deploymentlog user name
     */
    private String username;
    /**
     * Deploymentlog password
     */
    @ToString.Exclude
    private String password;

    @PostConstruct
    void init() {
        log.info("Deployment log client configuration: {}", this);
    }
}
