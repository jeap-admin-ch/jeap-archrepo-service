package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Every available artifact of one kind, with its entity tag")
public record ArtifactIndexDto(List<ArtifactIndexEntryDto> artifacts) {
}
