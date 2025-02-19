package ch.admin.bit.jeap.archrepo.web.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
class SystemDto {
    String name;
    String description;
    String ownedBy;
    Set<String> aliases;
    List<SystemComponentDto> systemComponents;
}
