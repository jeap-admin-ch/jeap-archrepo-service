package ch.admin.bit.jeap.archrepo.importer.prometheus.aws;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;

@AutoConfiguration
@EnableRetry
@ComponentScan
@ConditionalOnProperty("prometheus-aws.host")
public class AWSImporterConfiguration {

}
