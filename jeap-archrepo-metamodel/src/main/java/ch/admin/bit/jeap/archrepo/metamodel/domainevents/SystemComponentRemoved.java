package ch.admin.bit.jeap.archrepo.metamodel.domainevents;

import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import lombok.Value;

@Value
public class SystemComponentRemoved {
    String systemComponentName;

    public static SystemComponentRemoved of(SystemComponent component) {
        return new SystemComponentRemoved(component.getName());
    }
}
