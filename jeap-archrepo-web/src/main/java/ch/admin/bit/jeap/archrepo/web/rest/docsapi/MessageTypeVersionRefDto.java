package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One version of a message type, its entity tag, and where to read it")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageTypeVersionRefDto(
        @Schema(example = "2.0.0") String version,
        @Schema(description = "Identical to the ETag header of the version resource, so a consumer can decide "
                              + "without a request whether it has to fetch. Absent for a version stored before "
                              + "the arch repo recorded the hash, until the next model update",
                example = "\"sha256:41ab7c\"") String etag,
        @Schema(description = "Path of the version resource, relative to the service root") String contentUrl) {
}
