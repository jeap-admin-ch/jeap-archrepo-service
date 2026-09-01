package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Every message type of the model, with the versions it has")
public record MessageTypeIndexDto(List<MessageTypeIndexEntryDto> messageTypes) {
}
