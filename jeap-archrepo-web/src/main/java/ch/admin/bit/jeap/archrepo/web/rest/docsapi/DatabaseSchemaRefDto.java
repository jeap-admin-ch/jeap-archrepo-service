package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

@Schema(description = "Pointer to the database schema of a component")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DatabaseSchemaRefDto(
        @Schema(example = "42") String schemaVersion,
        @Schema(description = "Path of the schema content resource, relative to the service root")
        String contentUrl) {
}
