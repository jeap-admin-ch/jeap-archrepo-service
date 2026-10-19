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
     * Id of the root page under which all systems are listed
     */
    private String rootPageId;

    /**
     * Key of the confluence space into which the documentation is generated
     */
    private String spaceKey;

    private String url;

    private String username;

    @ToString.Exclude
    private String password;

    private boolean mockConfluenceClient = false;

    private String vizJsUrl = "https://cdn.jsdelivr.net/npm/@viz-js/viz@3.28.0/+esm";

    private String svgPanZoomJsUrl = "https://cdn.jsdelivr.net/npm/svg-pan-zoom@3.6.2/+esm";

    @PostConstruct
    void init() {
        log.info("Confluence configuration: {}", this);
    }
}
