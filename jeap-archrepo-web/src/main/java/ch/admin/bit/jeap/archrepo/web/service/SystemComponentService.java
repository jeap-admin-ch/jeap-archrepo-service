package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentFactory;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.persistence.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service class for managing system components.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemComponentService {

    private final TeamRepository teamRepository;
    private final SystemRepository systemRepository;
    private final SystemComponentRepository systemComponentRepository;

    /**
     * Finds or creates a system component based on the given name.
     *
     * @param componentName the name of the system component
     * @return the found or newly created SystemComponent
     */
    @Transactional
    public SystemComponent findOrCreateSystemComponent(String componentName) {
        log.info("Retrieve system component: {}", componentName);
        return systemComponentRepository.findByNameIgnoreCase(componentName).orElseGet(() -> createSystemComponent(componentName));
    }

    @Transactional(readOnly = true)
    public Optional<SystemComponent> findSystemComponent(String componentName) {
        log.info("Retrieve system component: {}", componentName);
        return systemComponentRepository.findByNameIgnoreCase(componentName);
    }

    private SystemComponent createSystemComponent(String componentName) {
        System system = findOrCreateSystem(extractSystemName(componentName));

        // If this component is imported by grafana afterward, the importer 'REST_CONTROLLER' is replaced by 'GRAFANA'.
        SystemComponent systemComponent = SystemComponentFactory.createSystemComponent(system, componentName, Importer.REST_CONTROLLER);
        log.info("Created system component: {} in system: {}", systemComponent.getName(), system.getName());
        return systemComponent;
    }

    private String extractSystemName(String componentName) {
        return componentName.split("-")[0];
    }

    private System findOrCreateSystem(String systemName) {
        return systemRepository.findByNameIgnoreCase(systemName)
                .or(() -> findSystemAliases(systemName))
                .orElseGet(() -> createSystem(systemName));
    }

    private System createSystem(String systemName) {
        log.info("Creating system and team: {}", systemName);
        return systemRepository.save(
                System.builder()
                        .name(systemName)
                        .defaultOwner(teamRepository.save(Team.builder().name(systemName).build()))
                        .build());
    }

    public Optional<System> findSystemAliases(String systemName) {
        return systemRepository.findAll().stream().filter(system -> matchesSystemNameOrAlias(systemName, system)).findFirst();
    }

    private boolean matchesSystemNameOrAlias(String systemName, System system) {
        return systemName.equalsIgnoreCase(system.getName()) ||
               (system.getAliases() != null && system.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(systemName)));
    }
}
