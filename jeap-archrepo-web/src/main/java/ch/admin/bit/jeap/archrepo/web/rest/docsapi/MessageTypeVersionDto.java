package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Everything the arch repo knows about one version of a message type")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageTypeVersionDto(
        String system,
        @Schema(example = "WvsDeclarationAcceptedEvent") String message,
        @Schema(example = "2.0.0") String version,
        @Schema(description = "The Avro compatibility this version declares against compatibleVersion",
                example = "BACKWARD") String compatibilityMode,
        @Schema(description = "The version this one is compatible with", example = "1.0.0") String compatibleVersion,
        @Schema(description = "The key schema, when the message type has one") MessageSchemaDto key,
        @Schema(description = "The value schema") MessageSchemaDto value) {
}
