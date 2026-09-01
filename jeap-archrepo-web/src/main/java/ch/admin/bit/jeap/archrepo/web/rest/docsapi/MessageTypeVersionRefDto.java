package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One version of a message type, and where to read it")
public record MessageTypeVersionRefDto(
        @Schema(example = "2.0.0") String version,
        @Schema(description = "Path of the version resource, relative to the service root") String contentUrl) {
}
