package ch.admin.bit.jeap.archrepo.importer.pactbroker;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan
@ConditionalOnProperty("pactbroker.url")
public class PactBrokerImporterConfiguration {
}
