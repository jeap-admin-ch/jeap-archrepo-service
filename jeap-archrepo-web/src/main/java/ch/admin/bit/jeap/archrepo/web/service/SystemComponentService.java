package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentFactory;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
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

    @TransactionalReadReplica
    public Optional<SystemComponent> findSystemComponent(String componentName) {
        log.info("Retrieve system component: {}", componentName);
        return systemComponentRepository.findByNameIgnoreCase(componentName);
    }

    private SystemComponent createSystemComponent(String componentName) {
        String systemName = extractSystemName(componentName);
        System system = systemRepository.findByNameIgnoreCase(systemName)
                .or(() -> findSystemAliases(systemName))
                .orElseThrow(() -> {
                    log.error("Cannot create component '{}': system '{}' is not onboarded", componentName, systemName);
                    return new UnknownSystemException(componentName, systemName);
                });

        // If this component is imported by grafana afterward, the importer 'REST_CONTROLLER' is replaced by 'GRAFANA'.
        SystemComponent systemComponent = SystemComponentFactory.createSystemComponent(system, componentName, Importer.REST_CONTROLLER);
        log.info("Created system component: {} in system: {}", systemComponent.getName(), system.getName());
        return systemComponent;
    }

    private String extractSystemName(String componentName) {
        return componentName.split("-")[0];
    }

    public Optional<System> findSystemAliases(String systemName) {
        return systemRepository.findAll().stream().filter(system -> matchesSystemNameOrAlias(systemName, system)).findFirst();
    }

    private boolean matchesSystemNameOrAlias(String systemName, System system) {
        return systemName.equalsIgnoreCase(system.getName()) ||
               (system.getAliases() != null && system.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(systemName)));
    }
}
