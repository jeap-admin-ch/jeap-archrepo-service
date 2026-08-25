package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@Schema(description = "One system with its components and relations")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SystemDetailDto(
        @Schema(example = "wvs") String name,
        String description,
        List<String> aliases,
        TeamDto team,
        List<ComponentDto> components,
        @Schema(description = "The active relations defined by this system, in both directions")
        List<RelationDto> relations) {
}
