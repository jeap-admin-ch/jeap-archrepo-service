package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "One message type, with the versions it has")
public record MessageTypeIndexEntryDto(
        @Schema(description = "The system that defines the message type", example = "wvs") String system,
        @Schema(example = "WvsDeclarationAcceptedEvent") String message,
        @Schema(description = "EVENT or COMMAND", example = "EVENT") String kind,
        @Schema(description = "Every version of this message type, sorted") List<MessageTypeVersionRefDto> versions) {
}
