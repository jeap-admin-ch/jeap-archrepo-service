package ch.admin.bit.jeap.archrepo.docgen.graph.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReactionNodeDto implements NodeDto {
    private long id;
    private String component;
    private boolean isHighlighted = false;

    @Override
    public String getDotId() {
        return NodeDtoType.REACTION.name() + "-" + id;
    }

    @Override
    public String toDot() {
        String style = isHighlighted ? ", style=filled, fillcolor=lightblue" : "";
        String componentName = component.replaceAll("(-scs|-service|-component)$", "");
        String label = String.format("%s\\n%d", componentName, id);
        return String.format("  \"%s\" [label=\"%s\", shape=box, style=rounded%s];", getDotId(), label, style);
    }
}