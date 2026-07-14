package ch.admin.bit.jeap.archrepo.docgen.graph.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReactionNodeDto implements NodeDto {
    private long id;
    private String component;
    private Boolean highlighted = false;
    private Boolean partOfCluster = false;

    @Override
    public String getDotId() {
        return NodeDtoType.REACTION.name() + "-" + id;
    }

    @Override
    public String toDot() {
        return toDot(null);
    }

    @Override
    public String toDot(String url) {
        String style = Boolean.TRUE.equals(highlighted) ? ", style=filled, fillcolor=lightblue" : "";
        String label = Boolean.TRUE.equals(partOfCluster)
                ? String.valueOf(id)
                : NodeDto.escapeDotString(component.replaceAll("(-scs|-service)$", "")) + "\\n" + id;
        return String.format("  \"%s\" [label=\"%s\", shape=box, style=rounded%s%s];",
                getDotId(), label, style, NodeDto.linkAttributes(url));
    }
}
