package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

@Schema(description = "The team owning a system or a component")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeamDto(
        @Schema(example = "Team Blue") String name,
        @Schema(example = "team-blue@example.com") String contactAddress,
        @Schema(example = "https://jira.example.com/projects/WVS") String jiraLink,
        @Schema(example = "https://confluence.example.com/display/WVS") String confluenceLink) {
}
