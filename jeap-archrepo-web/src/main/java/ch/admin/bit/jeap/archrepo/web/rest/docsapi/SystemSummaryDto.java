package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@Schema(description = "A system, without its components and relations")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SystemSummaryDto(
        @Schema(example = "wvs") String name,
        String description,
        @Schema(description = "Further names the system is known under") List<String> aliases,
        TeamDto team) {
}
