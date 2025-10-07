package ch.admin.bit.jeap.archrepo.docgen.graph.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "nodeType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MessageNodeDto.class, name = "MESSAGE"),
        @JsonSubTypes.Type(value = ReactionNodeDto.class, name = "REACTION")
})
public interface NodeDto {
    long getId();

    String getDotId();

    @JsonIgnore
    String toDot();
}
