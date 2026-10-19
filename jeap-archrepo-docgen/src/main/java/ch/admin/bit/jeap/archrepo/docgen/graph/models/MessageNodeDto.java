package ch.admin.bit.jeap.archrepo.docgen.graph.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageNodeDto implements NodeDto {
    private long id;
    private String messageType;
    private String variant;
    private Boolean highlighted = false;

    @Override
    public String getDotId() {
        return NodeDtoType.MESSAGE.name() + "-" + id;
    }

    @Override
    public String toDot() {
        return toDot(null);
    }

    @Override
    public String toDot(String url) {
        String style = Boolean.TRUE.equals(highlighted) ? ", style=filled, fillcolor=lightblue" : "";
        String messageNodeName = NodeDto.escapeDotString(messageType.replaceAll("(Event|Command)$", ""));
        String label = variant != null && !variant.isBlank()
                ? String.format("%s\\n[%s]", messageNodeName, NodeDto.escapeDotString(variant))
                : messageNodeName;
        return String.format("  \"%s\" [label=\"%s\", shape=ellipse%s%s];",
                getDotId(), label, style, NodeDto.linkAttributes(url));
    }
}
