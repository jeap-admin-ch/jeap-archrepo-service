package ch.admin.bit.jeap.archrepo.docgen.graph.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "edgeType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TriggerEdgeDto.class, name = "TRIGGER"),
        @JsonSubTypes.Type(value = ActionEdgeDto.class, name = "ACTION")
})
public interface EdgeDto {
    @JsonIgnore
    String toDot();
}