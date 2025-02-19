package ch.admin.bit.jeap.archrepo.importer.messagetype;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@EnableConfigurationProperties(MessageTypeImporterProperties.class)
@ConditionalOnProperty("messages.message-contract-service-uri")
@ComponentScan
public class MessageTypeImporterConfiguration {
}
