package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A REST endpoint provided by a component")
public record RestApiDto(
        @Schema(example = "GET") String method,
        @Schema(example = "/api/foo/{id}") String path) {
}
