package ch.admin.bit.jeap.archrepo.metamodel.system;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import lombok.experimental.UtilityClass;

import java.time.ZonedDateTime;

@UtilityClass
public class SystemComponentFactory {

    public SystemComponent createSystemComponent(System system, String cfAppName, Importer importer) {
        SystemComponent systemComponent = createByName(cfAppName);
        systemComponent.setImporter(importer);
        systemComponent.setOwnedBy(system.getDefaultOwner());
        systemComponent.setLastSeenFromDate(ZonedDateTime.now());
        system.addSystemComponent(systemComponent);
        return systemComponent;
    }

    private SystemComponent createByName(String cfAppName) {
        if (cfAppName.endsWith("-ui") || cfAppName.endsWith("-frontend") || cfAppName.endsWith("-webui")) {
            return new Frontend(cfAppName);
        } else if (cfAppName.endsWith("-scs")) {
            return new SelfContainedSystem(cfAppName);
        } else {
            return new BackendService(cfAppName);
        }
    }
}
