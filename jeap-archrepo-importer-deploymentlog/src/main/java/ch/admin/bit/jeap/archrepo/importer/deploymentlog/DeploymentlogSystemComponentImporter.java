package ch.admin.bit.jeap.archrepo.importer.deploymentlog;

import ch.admin.bit.jeap.archrepo.importer.deploymentlog.client.ComponentVersionSummaryDto;
import ch.admin.bit.jeap.archrepo.importer.deploymentlog.client.DeploymentlogService;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Slf4j
class DeploymentlogSystemComponentImporter implements ArchRepoImporter {

    private final DeploymentlogService deploymentlogService;

    DeploymentlogSystemComponentImporter(DeploymentlogService deploymentlogService) {
        this.deploymentlogService = deploymentlogService;
    }

    @Override
    public int getOrder() {
        // Lowest precedence, only imports components if they are not found using metrics, the
        // SystemComponentImporters have a higher precedence.
        return Integer.MIN_VALUE;
    }

    @Override
    public void importIntoModel(ArchitectureModel architectureModel, String environment) {
        log.info("Getting components from the deployment log...");
        List<ComponentVersionSummaryDto> components = deploymentlogService.getDeployedComponents(environment);
        components.forEach(component -> importComponent(architectureModel, component));
        removeDeletedComponentsFromArchitectureModel(architectureModel, components);
    }

    private void importComponent(ArchitectureModel architectureModel, ComponentVersionSummaryDto component) {
        String componentName = component.componentName();
        String systemName = componentName.split("-")[0];
        Optional<System> system = architectureModel.findSystem(systemName);
        if (system.isEmpty()) {
            log.warn("Skipped system component {}: System with name {} not found in model", componentName, systemName);
            return;
        }
        if (component.undeployed()) {
            markUndeployedIfPresent(system.get(), component);
        } else {
            createOrRedeploySystemComponent(system.get(), componentName);
        }
    }

    private void createOrRedeploySystemComponent(System system, String componentName) {
        Optional<SystemComponent> existing = system.findSystemComponent(componentName);
        if (existing.isEmpty()) {
            log.info("Create new system component from deploymentlog {} in system {}", componentName, system.getName());
            SystemComponentFactory.createSystemComponent(system, componentName, Importer.DEPLOYMENT_LOG);
        } else if (existing.get().isUndeployed()) {
            log.info("System component {} in system {} was redeployed", componentName, system.getName());
            existing.get().markDeployed();
        } else {
            log.info("System component {} in system {} already exists", componentName, system.getName());
        }
    }

    private void markUndeployedIfPresent(System system, ComponentVersionSummaryDto component) {
        system.findSystemComponent(component.componentName()).ifPresentOrElse(
                systemComponent -> {
                    if (!systemComponent.isUndeployed()) {
                        log.info("Marking system component {} in system {} as undeployed", component.componentName(), system.getName());
                    }
                    systemComponent.markUndeployed(component.undeployedAt());
                },
                () -> log.info("Skipped undeployed system component {}: not found in model", component.componentName()));
    }

    private void removeDeletedComponentsFromArchitectureModel(ArchitectureModel architectureModel, List<ComponentVersionSummaryDto> importedComponents) {
        Set<String> importedNames = importedComponents.stream()
                .map(ComponentVersionSummaryDto::componentName)
                .collect(toSet());
        // Remove components imported only by DeploymentLog. If a component is imported by grafana, the importer
        // 'DeploymentLog' is replaced by 'Grafana'. An undeployed component is still reported by the deployment log
        // (as a tombstone) and is therefore kept here, only marked undeployed above; a component is removed only
        // once the deployment log no longer reports it at all.
        for (SystemComponent systemComponent : architectureModel.getAllSystemComponentsByImporter(Importer.DEPLOYMENT_LOG)) {
            if (!importedNames.contains(systemComponent.getName())) {
                architectureModel.remove(systemComponent);
            }
        }
    }
}
