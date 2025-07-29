package ch.admin.bit.jeap.archrepo.web.rest.model;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class DeleteRestApiDto {
    @NotNull
    String providerName;
    @NotNull
    String consumerName;
    @NotNull
    String method;
    @NotNull
    String path;

}
