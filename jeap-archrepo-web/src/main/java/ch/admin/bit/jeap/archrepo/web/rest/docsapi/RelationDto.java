package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

@Schema(description = "A relation between two components")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RelationDto(
        RelationType type,
        String consumerSystem,
        String consumer,
        String providerSystem,
        String provider,
        @Schema(description = "REST relations only") String method,
        @Schema(description = "REST relations only") String path,
        @Schema(description = "REST relations only, when a Pact contract exists") String pactUrl,
        @Schema(description = "Event and command relations only") String messageType) {
}
