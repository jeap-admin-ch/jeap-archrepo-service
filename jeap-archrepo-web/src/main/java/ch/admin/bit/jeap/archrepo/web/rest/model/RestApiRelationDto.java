package ch.admin.bit.jeap.archrepo.web.rest.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RestApiRelationDto {

    String consumerSystem;

    String consumer;

    String providerSystem;

    String provider;

    String method;

    String path;
}
