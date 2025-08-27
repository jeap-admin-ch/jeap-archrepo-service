package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos;

import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.RhosGrafanaClient;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Imports applications (SystemComponent) defined in RHOS into the architecture model.
 * <p>
 * This importer reads data published as metrics by services from Grafana.
 * {@link RhosGrafanaClient#services(String, String)}
 * <p>
 * If the system is not found, the service is not imported in the architecture model.
 */
@Component
@Slf4j
@AllArgsConstructor
class RhosSystemComponentImporter implements ArchRepoImporter {

    private final RhosGrafanaClient grafanaClient;
    private final RhosSystemNameExtractor systemNameExtractor;

    @Override
    public int getOrder() {
        // Higher precedence than the DeploymentLogSystemComponentImporter, which is the only other importer that
        // also creates system components. Creating components from metrics is preferred as this represents a live
        // signal from a running component vs. something that might have been deployed.
        return Integer.MIN_VALUE + 50;
    }

    @Override
    public void importIntoModel(ArchitectureModel architectureModel, String environment) {
        log.info("Reading available matching namespaces from Prometheus...");
        Set<String> namespaces = grafanaClient.namespaces(environment);
        log.info("Found namespaces {} on stage {}", namespaces, environment);

        for (String namespace : namespaces) {
            Optional<String> systemNameOptional = systemNameExtractor.extractSystemName(namespace);
            if (systemNameOptional.isEmpty()) {
                log.warn("Could not extract system name from namespace {}", namespace);
                continue;
            }
            String systemName = systemNameOptional.get();
            Optional<System> system = architectureModel.findSystem(systemName);
            if (system.isPresent()) {
                log.info("Importing SystemComponents from system {}", systemName);
                importSystemComponent(architectureModel, systemName, environment, namespace);
            } else {
                log.warn("Skipped System {} (not found in model)", systemName);
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

    private void importSystemComponent(ArchitectureModel architectureModel, String systemName, String environment, String namespace) {
        log.info("Importing SystemComponents from {} from namespace {}", systemName, namespace);
        Set<String> serviceNames = grafanaClient.services(environment, namespace);
        serviceNames.forEach(serviceName -> importSystemComponentIntoSystem(architectureModel, systemName, serviceName));
    }

    private void importSystemComponentIntoSystem(ArchitectureModel architectureModel, String systemName, String componentName) {
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
