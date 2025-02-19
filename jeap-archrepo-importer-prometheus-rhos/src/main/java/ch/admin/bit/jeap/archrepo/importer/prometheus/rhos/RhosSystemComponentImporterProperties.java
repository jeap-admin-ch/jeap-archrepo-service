package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Data
@Configuration
@ConfigurationProperties(prefix = "rhos-system-component-importer", ignoreUnknownFields = false)
@Slf4j
public class RhosSystemComponentImporterProperties {

    /**
     * Name of RHOS stages to use to import services from
     */
    private Set<String> importedStages;


    @PostConstruct
    void init() {
        log.info("SystemComponentImporter configuration: {}", this);
    }
}
