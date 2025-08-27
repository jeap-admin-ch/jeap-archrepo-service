package ch.admin.bit.jeap.archrepo.web;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "archrepo-config")
@AllArgsConstructor
@Slf4j
@Validated
public class ArchRepoConfigProperties {

    /**
     * Current environment of the Gov-Dashboard.
     * This environment is used for requests to data sources that manage environment specific data:
     * - requests to Prometheus (metrics)
     * - Deployment Log (deployment versions)
     */
    @NotNull
    ArchRepoEnvironment environment;

    @PostConstruct
    void init() {
        log.info("ArchRepoConfigProperties configuration: {}", this);
    }

}
