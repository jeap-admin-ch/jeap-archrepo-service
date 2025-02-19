package ch.admin.bit.jeap.archrepo.web.rest.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CreateSystemDto {
    @NotNull
    String name;
    String description;
    String confluenceLink;
    List<String> aliases;
    @NotNull
    String teamName;

}
