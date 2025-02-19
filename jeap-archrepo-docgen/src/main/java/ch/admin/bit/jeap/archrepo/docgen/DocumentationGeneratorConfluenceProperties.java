package ch.admin.bit.jeap.archrepo.docgen;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "archrepo.documentation-generator.confluence", ignoreUnknownFields = false)
@Slf4j
public class DocumentationGeneratorConfluenceProperties {

    /**
     * Name of the root page under which all systems are listed
     */
    private String rootPageName;

    /**
     * Key of the confluence space into which the documentation is generated
     */
    private String spaceKey;

    private String url;

    private String username;

    @ToString.Exclude
    private String password;

    private boolean mockConfluenceClient = false;

    @PostConstruct
    void init() {
        log.info("Confluence configuration: {}", this);
    }
}
