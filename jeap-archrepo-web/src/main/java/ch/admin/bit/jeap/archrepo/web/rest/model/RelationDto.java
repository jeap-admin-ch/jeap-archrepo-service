package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelationDto {

    RelationType relationType;

    String consumerSystem;

    String consumer;

    String providerSystem;

    String provider;

    String method;

    String path;

    String messageType;

}
