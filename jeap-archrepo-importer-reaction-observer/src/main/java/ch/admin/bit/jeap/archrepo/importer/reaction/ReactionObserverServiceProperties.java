package ch.admin.bit.jeap.archrepo.importer.reaction;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "reactionobserverservice", ignoreUnknownFields = false)
@Slf4j
public class ReactionObserverServiceProperties {

    /**
     * URL of Reaction Observer Service
     */
    private String url;
    /**
     * Username for Reaction Observer Service
     */
    private String username;
    /**
     * Password for Reaction Observer Service
     */
    @ToString.Exclude
    private String password;

    @PostConstruct
    void init() {
        log.info("Reaction Observer Service client configuration: {}", this);
    }
}
