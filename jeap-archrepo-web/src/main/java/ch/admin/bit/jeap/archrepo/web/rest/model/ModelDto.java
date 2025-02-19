package ch.admin.bit.jeap.archrepo.web.rest.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
class ModelDto {
    List<SystemDto> systems;
}
