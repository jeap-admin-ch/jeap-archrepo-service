package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos;

import ch.admin.bit.jeap.archrepo.importer.prometheus.condition.ConditionalOnNonEmptyHosts;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;

@AutoConfiguration
@EnableRetry
@ComponentScan
@ConditionalOnNonEmptyHosts(propertyName = "prometheus-rhos.hosts")
public class RhosImporterConfiguration {
}