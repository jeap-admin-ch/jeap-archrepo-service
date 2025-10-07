package ch.admin.bit.jeap.archrepo.docgen.graph.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TriggerEdgeDto implements EdgeDto {
    private long sourceId;
    private NodeDtoType sourceNodeType;
    private long targetReactionId;
    private Integer median;

    @Override
    public String toDot() {
        String source = NodeDtoType.MESSAGE.name() + "-" + sourceId;
        String target = NodeDtoType.REACTION.name() + "-" + targetReactionId;
        int penWidth = switch (String.valueOf(median).length()) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> 1;
        };
        return String.format("  \"%s\" -> \"%s\" [penwidth=%d];", source, target, penWidth);
    }
}