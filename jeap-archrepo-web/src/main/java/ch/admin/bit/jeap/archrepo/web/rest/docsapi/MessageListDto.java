package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "The events and commands defined by a system")
public record MessageListDto(List<MessageDto> messages) {
}
