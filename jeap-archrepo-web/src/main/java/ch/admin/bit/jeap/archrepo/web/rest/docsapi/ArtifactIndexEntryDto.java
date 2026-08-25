package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;

@Schema(description = "One available artifact, with the entity tag of its content")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ArtifactIndexEntryDto(
        String system,
        String component,
        @Schema(description = "The artifact's version as published", example = "1.4.2") String version,
        @Schema(description = "Byte-identical to the ETag header of the content resource, so a consumer can "
                              + "decide without a request whether it has to fetch",
                example = "\"sha256:41ab7c\"") String etag,
        ZonedDateTime lastModifiedAt,
        @Schema(description = "Path of the content resource, relative to the service root") String contentUrl) {
}
