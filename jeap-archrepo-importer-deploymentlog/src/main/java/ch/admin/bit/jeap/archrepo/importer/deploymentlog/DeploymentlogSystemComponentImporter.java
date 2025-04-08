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

    private final DeploymentlogProperties properties;
    private final DeploymentlogService deploymentlogService;

    DeploymentlogSystemComponentImporter(DeploymentlogProperties properties, DeploymentlogService deploymentlogService) {
        this.properties = properties;
        this.deploymentlogService = deploymentlogService;
    }

    @Override
    public int getOrder() {
        // Lowest precedence, only imports components if they are not found using metrics, the
        // SystemComponentImporters have a higher precedence.
        return Integer.MIN_VALUE;
    }

    @Override
    public void importIntoModel(ArchitectureModel architectureModel) {
        log.info("Getting components from the deployment log...");
        List<ComponentVersionSummaryDto> components = deploymentlogService.getDeployedComponents(properties.getEnvironment());
        components.forEach(component -> importComponent(architectureModel, component));
        removeDeletedComponentsFromArchitectureModel(architectureModel, components);
    }

    private void importComponent(ArchitectureModel architectureModel, ComponentVersionSummaryDto component) {
        String componentName = component.componentName();
        String systemName = componentName.split("-")[0];
        Optional<System> system = architectureModel.findSystem(systemName);
        if (system.isPresent()) {
            createSystemComponentIfNotExists(system.get(), componentName);
        } else {
            log.warn("Skipped system component {}: System with name {} not found in model", componentName, systemName);
        }
    }

    private void createSystemComponentIfNotExists(System system, String componentName) {
        if (system.findSystemComponent(componentName).isEmpty()) {
            log.info("Create new system component from deploymentlog {} in system {}", componentName, system.getName());
            SystemComponentFactory.createSystemComponent(system, componentName, Importer.DEPLOYMENT_LOG);
        } else {
            log.info("System component {} in system {} already exists", componentName, system.getName());
        }
    }

    private void removeDeletedComponentsFromArchitectureModel(ArchitectureModel architectureModel, List<ComponentVersionSummaryDto> importedComponents) {
        Set<String> importedNames = importedComponents.stream()
                .map(ComponentVersionSummaryDto::componentName)
                .collect(toSet());
        // Remove components imported only by DeploymentLog. If a component is imported by grafana, the importer 'DeploymentLog' is replaced by 'Grafana'.
        for (SystemComponent systemComponent : architectureModel.getAllSystemComponentsByImporter(Importer.DEPLOYMENT_LOG)) {
            if (!importedNames.contains(systemComponent.getName())) {
                architectureModel.remove(systemComponent);
            }
        }
    }
}
