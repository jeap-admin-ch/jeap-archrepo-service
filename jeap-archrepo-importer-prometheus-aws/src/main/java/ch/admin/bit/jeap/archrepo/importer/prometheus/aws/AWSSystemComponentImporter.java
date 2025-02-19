package ch.admin.bit.jeap.archrepo.importer.prometheus.aws;

import ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client.AWSPrometheusClient;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Imports applications (SystemComponent) defined in AWS into the architecture model.
 * <p>
 * This importer reads data published as metrics by applications from prometheus.
 * {@link AWSPrometheusClient#listApplications() (String, String)})
 * <p>
 * If the system is not found, the service is not imported in the architecture model.
 */
@Component
@Slf4j
class AWSSystemComponentImporter implements ArchRepoImporter {

    private final AWSSystemComponentImporterProperties importerProperties;
    private final AWSPrometheusClient awsPrometheusClient;

    AWSSystemComponentImporter(AWSSystemComponentImporterProperties importerProperties, AWSPrometheusClient awsPrometheusClient) {
        this.importerProperties = importerProperties;
        this.awsPrometheusClient = awsPrometheusClient;
    }

    @Override
    public int getOrder() {
        // Higher precedence than the DeploymentLogSystemComponentImporter, which is the only other importer that
        // also creates system components. Creating components from metrics is preferred as this represents a live
        // signal from a running component vs. something that might have been deployed.
        return Integer.MIN_VALUE + 50;
    }

    @Override
    public void importIntoModel(ArchitectureModel architectureModel) {
        log.info("Reading available matching applications from Prometheus...");
        Set<String> applications = awsPrometheusClient.listApplications();
        log.info("Found applications {}", applications);

        for (String application : applications) {
            Optional<System> system = architectureModel.findSystem(application);
            if (system.isPresent()) {
                log.info("Importing SystemComponents from application {}", application);
                importSystemComponent(architectureModel, application);
            } else {
                log.warn("Skipped Application {} (not found in model)", application);
            }
        }

        removeFromArchitectureModel(architectureModel);
    }

    private void removeFromArchitectureModel(ArchitectureModel architectureModel) {
        log.info("Remove old SystemComponents without metrics...");
        List<SystemComponent> allSystemComponentsByImporter = architectureModel.getAllSystemComponentsByImporter(Importer.GRAFANA);
        log.info("Found {} SystemComponents in model", allSystemComponentsByImporter.size());

        for (SystemComponent systemComponent : allSystemComponentsByImporter) {
            if (systemComponent.isObsolete()) {
                log.info("Remove SystemComponent {} with lastSeen {}", systemComponent.getName(), systemComponent.getLastSeen());
                architectureModel.remove(systemComponent);
            }
        }
    }

    private void importSystemComponent(ArchitectureModel architectureModel, String application) {
        Set<String> availableStages;
        try {
            availableStages = awsPrometheusClient.listStages(application);
        } catch (IllegalArgumentException ex) {
            log.warn("Application {} does not exist on AWS", application);
            return;
        }
        Set<String> importedStages = new HashSet<>(importerProperties.getImportedStages());
        importedStages.retainAll(availableStages);
        log.info("Importing SystemComponents from {} from stages {}", application, importedStages);
        importedStages.forEach(stage -> importSystemComponentFromApplicationAndStage(architectureModel, application, stage));
    }

    private void importSystemComponentFromApplicationAndStage(ArchitectureModel architectureModel, String application, String stage) {
        Set<String> services = awsPrometheusClient.listServices(application, stage);
        services.forEach(service -> importSystemComponentIntoSystem(architectureModel, service));
    }

    private void importSystemComponentIntoSystem(ArchitectureModel architectureModel, String componentName) {
        String systemName = componentName.split("-")[0];
        Optional<System> system = architectureModel.findSystem(systemName);
        if (system.isPresent()) {
            createOrUpdateSystemComponent(system.get(), componentName);
        } else {
            log.warn("Skipped SystemComponent {}: System with name {} not found in model", componentName, systemName);
        }
    }

    private void createOrUpdateSystemComponent(System system, String componentName) {
        if (system.findSystemComponent(componentName).isEmpty()) {
            log.info("Create new SystemComponent {} in System {}", componentName, system.getName());
            SystemComponentFactory.createSystemComponent(system, componentName, Importer.GRAFANA);
        } else {
            log.info("Update SystemComponent {} in System {}", componentName, system.getName());
            SystemComponent systemComponent = system.findSystemComponent(componentName).get();
            // When a component is imported from metrics, the grafana importer takes precedence over any other importers
            systemComponent.setImporter(Importer.GRAFANA);
            systemComponent.setLastSeenFromDate(ZonedDateTime.now());
        }
    }
}
