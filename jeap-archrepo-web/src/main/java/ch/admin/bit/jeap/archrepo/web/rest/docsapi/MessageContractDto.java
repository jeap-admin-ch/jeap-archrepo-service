package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@Schema(description = "A component's contract on a message")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageContractDto(
        @Schema(description = "PUBLISHER or CONSUMER for an event, SENDER or RECEIVER for a command",
                example = "PUBLISHER") String role,
        String component,
        @Schema(description = "The system owning the component, when it is known") String system,
        String topic,
        List<String> versions) {
}
