package ch.admin.bit.jeap.archrepo.importer.prometheus.aws;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "aws-system-component-importer", ignoreUnknownFields = false)
@Slf4j
public class AWSSystemComponentImporterProperties {

    /**
     * Name of AWS stages to use to import services from
     */
    private List<String> importedStages;

    @PostConstruct
    void init() {
        log.info("SystemComponentImporter configuration: {}", this);
    }
}
