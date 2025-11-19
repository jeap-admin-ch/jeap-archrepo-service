package ch.admin.bit.jeap.archrepo.metamodel.system;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import lombok.experimental.UtilityClass;

import java.time.ZonedDateTime;

@UtilityClass
public class SystemComponentFactory {

    public SystemComponent createSystemComponent(System system, String componentName, Importer importer) {
        SystemComponent systemComponent = createByName(componentName);
        systemComponent.setImporter(importer);
        systemComponent.setOwnedBy(system.getDefaultOwner());
        systemComponent.setLastSeenFromDate(ZonedDateTime.now());
        system.addSystemComponent(systemComponent);
        return systemComponent;
    }

    private SystemComponent createByName(String componentName) {
        if (componentName.endsWith("-ui") || componentName.endsWith("-frontend") || componentName.endsWith("-webui")) {
            return new Frontend(componentName);
        } else if (componentName.endsWith("-scs")) {
            return new SelfContainedSystem(componentName);
        } else {
            return new BackendService(componentName);
        }
    }
}
