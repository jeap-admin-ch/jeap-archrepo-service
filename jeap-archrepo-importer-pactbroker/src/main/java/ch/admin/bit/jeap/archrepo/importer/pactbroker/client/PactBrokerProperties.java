package ch.admin.bit.jeap.archrepo.importer.pactbroker.client;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "pactbroker", ignoreUnknownFields = false)
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public
class PactBrokerProperties {
    private String url;
    private String username;
    @ToString.Exclude
    private String password;

    @PostConstruct
    void init() {
        log.info("Pactbroker configuration: {}", this);
    }
}
