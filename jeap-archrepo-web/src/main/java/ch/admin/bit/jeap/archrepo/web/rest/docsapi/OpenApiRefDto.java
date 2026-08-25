package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

@Schema(description = "Pointer to the OpenAPI spec of a component")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenApiRefDto(
        @Schema(example = "1.4.2") String version,
        @Schema(example = "https://foo-bar.example.com") String serverUrl,
        @Schema(description = "Path of the spec content resource, relative to the service root")
        String contentUrl,
        @Schema(description = "Absolute deep link into the Swagger UI, for a reader of the generated documentation")
        String swaggerUrl) {
}
