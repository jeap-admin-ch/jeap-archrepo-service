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
    private boolean highlighted = false;
    private boolean partOfCluster = false;

    @Override
    public String getDotId() {
        return NodeDtoType.REACTION.name() + "-" + id;
    }

    @Override
    public String toDot() {
        String style = highlighted ? ", style=filled, fillcolor=lightblue" : "";
        String label = partOfCluster ? String.valueOf(id) : component.replaceAll("(-scs|-service)$", "") + "\\n" + id;
        return String.format("  \"%s\" [label=\"%s\", shape=box, style=rounded%s];", getDotId(), label, style);
    }
}
