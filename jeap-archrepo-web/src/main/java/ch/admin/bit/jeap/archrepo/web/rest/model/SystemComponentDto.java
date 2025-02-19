package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
class SystemComponentDto {
    String name;
    String description;
    String ownedBy;
    Importer importer;
    SystemComponentType type;
}
