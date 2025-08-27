package ch.admin.bit.jeap.archrepo.importer.prometheus.cf;

import ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client.CloudFoundryPrometheusClient;
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
 * Imports applications (SystemComponent) defined in organizations matching prefixes configured in
 * (see {@link CloudFoundrySystemComponentImporterProperties#getOrgPrefixes()}) into the architecture model.
 * <p>
 * This importer reads data published as metrics by applications from Grafana.
 * {@link CloudFoundryPrometheusClient#listApps(String, String)})
 * <p>
 * The system for organizations is determined by splitting the app name at the first dash,
 * i.e. system-app-scs will have the importer looking for a system named "system" in the model.
 * If the system is not found, the application is not imported in the architecture model.
 */
@Component
@Slf4j
class CloudFoundrySystemComponentImporter implements ArchRepoImporter {

    private static final String BLUE_GREEN_TEMPORARY_APP_POSTFIX = "-venerable";

    private final CloudFoundrySystemComponentImporterProperties systemComponentImporterProperties;
    private final CloudFoundryPrometheusClient cloudFoundryPrometheusClient;

    CloudFoundrySystemComponentImporter(CloudFoundrySystemComponentImporterProperties systemComponentImporterProperties, CloudFoundryPrometheusClient cloudFoundryPrometheusClient) {
        this.systemComponentImporterProperties = systemComponentImporterProperties;
        this.cloudFoundryPrometheusClient = cloudFoundryPrometheusClient;
    }

    @Override
    public int getOrder() {
        // Higher precedence than the DeploymentLogSystemComponentImporter, which is the only other importer that
        // also creates system components. Creating components from metrics is preferred as this represents a live
        // signal from a running component vs. something that might have been deployed.
        return Integer.MIN_VALUE + 50;
    }

    @Override
    public void importIntoModel(ArchitectureModel architectureModel, String environment) {
        log.info("Reading available matching organisations from Grafana...");
        Set<String> organisations = cloudFoundryPrometheusClient.listOrganisationWithMatchingPrefix(systemComponentImporterProperties.getOrgPrefixes(), environment);
        log.info("Importing SystemComponents from organisations {}", organisations);
        organisations.forEach(orgName -> importSystemComponentFromOrg(architectureModel, orgName, environment));
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

    private void importSystemComponentFromOrg(ArchitectureModel architectureModel, String orgName, String space) {
        log.info("Importing SystemComponents from {} from space {}", orgName, space);
        importSystemComponentFromOrgAndSpace(architectureModel, orgName, space);
    }

    private void importSystemComponentFromOrgAndSpace(ArchitectureModel architectureModel, String orgName, String space) {
        Set<String> apps = cloudFoundryPrometheusClient.listApps(orgName, space);
        apps.stream()
                .filter(app -> !app.endsWith(BLUE_GREEN_TEMPORARY_APP_POSTFIX))
                .forEach(app -> importSystemComponentIntoSystem(architectureModel, app));
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
