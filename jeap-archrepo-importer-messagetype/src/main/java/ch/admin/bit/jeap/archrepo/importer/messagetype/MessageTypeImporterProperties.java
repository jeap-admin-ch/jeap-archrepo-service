package ch.admin.bit.jeap.archrepo.importer.messagetype;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "messages", ignoreUnknownFields = false)
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class MessageTypeImporterProperties {
    private List<String> gitUris;
    private String messageContractServiceUri;

    @PostConstruct
    void init() {
        log.info("Message Type Registry Importer configuration: {}", this);
    }
}
