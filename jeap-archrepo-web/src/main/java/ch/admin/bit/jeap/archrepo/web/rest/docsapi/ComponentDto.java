package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentType;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;
import java.util.List;

@Schema(description = "A component of a system")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComponentDto(
        @Schema(example = "wvs-foo-bar-service") String name,
        String description,
        SystemComponentType type,
        @Schema(description = "The component's team, falling back to the system's default owner") TeamDto team,
        @Schema(description = "Which source this component was imported from") Importer importer,
        @Schema(description = "When the component was last seen by an importer; lets a page mark it stale")
        ZonedDateTime lastSeen,
        List<RestApiDto> restApis,
        @Schema(description = "Present only when an OpenAPI spec has been published") OpenApiRefDto openApi,
        @Schema(description = "Present only when a database schema has been published")
        DatabaseSchemaRefDto databaseSchema) {
}
