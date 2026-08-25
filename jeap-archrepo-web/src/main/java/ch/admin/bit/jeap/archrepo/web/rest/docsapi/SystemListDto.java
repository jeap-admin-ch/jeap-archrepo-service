package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Every system of the landscape")
public record SystemListDto(List<SystemSummaryDto> systems) {
}
