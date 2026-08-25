package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@Schema(description = "An event or a command defined by a system")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageDto(
        @Schema(example = "WvsDeclarationAcceptedEvent") String name,
        @Schema(description = "EVENT or COMMAND", example = "EVENT") String kind,
        String scope,
        String topic,
        String descriptorUrl,
        String documentationUrl,
        String description,
        List<String> versions,
        List<MessageContractDto> contracts) {
}
