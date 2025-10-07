package ch.admin.bit.jeap.archrepo.docgen.graph.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionEdgeDto implements EdgeDto {
    private long sourceReactionId;
    private long targetId;
    private NodeDtoType targetNodeType;

    @Override
    public String toDot() {
        String source = NodeDtoType.REACTION.name() + "-" + sourceReactionId;
        String target = NodeDtoType.MESSAGE.name() + "-" + targetId;
        return String.format("  \"%s\" -> \"%s\";", source, target);
    }
}