package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.importer.deploymentlog.DeploymentlogImporterConfiguration;
import ch.admin.bit.jeap.archrepo.importer.messagetype.MessageTypeImporterConfiguration;
import ch.admin.bit.jeap.archrepo.importer.pactbroker.PactBrokerImporterConfiguration;
import ch.admin.bit.jeap.archrepo.importer.prometheus.aws.AWSImporterConfiguration;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.RhosImporterConfiguration;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

@TestConfiguration
@EnableAutoConfiguration(exclude = {
        PactBrokerImporterConfiguration.class,
        AWSImporterConfiguration.class,
        RhosImporterConfiguration.class,
        MessageTypeImporterConfiguration.class,
        DeploymentlogImporterConfiguration.class})
@ComponentScan({"ch.admin.bit.jeap.archrepo.web", "ch.admin.bit.jeap.archrepo.persistence"})
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ArchRepoTestConfiguration {


}
