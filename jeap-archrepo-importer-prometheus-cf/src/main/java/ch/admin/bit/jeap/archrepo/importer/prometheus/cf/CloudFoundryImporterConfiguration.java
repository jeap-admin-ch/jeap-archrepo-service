package ch.admin.bit.jeap.archrepo.importer.prometheus.cf;

import ch.admin.bit.jeap.archrepo.importer.prometheus.condition.ConditionalOnNonEmptyHosts;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;

@AutoConfiguration
@EnableRetry
@ComponentScan
@ConditionalOnNonEmptyHosts(propertyName = "prometheus-cf.hosts")
public class CloudFoundryImporterConfiguration {

}
