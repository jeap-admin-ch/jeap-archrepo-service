package ch.admin.bit.jeap.archrepo.importer.prometheus.cf;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "system-component-importer", ignoreUnknownFields = false)
@Slf4j
public class CloudFoundrySystemComponentImporterProperties {
    /**
     * Organizations are only imported if they match one of these prefixes
     */
    private List<String> orgPrefixes;
    /**
     * Name of CF spaces to use to import apps from
     */
    private List<String> importedSpaces;

    @PostConstruct
    void init() {
        log.info("SystemComponentImporter configuration: {}", this);
    }
}
