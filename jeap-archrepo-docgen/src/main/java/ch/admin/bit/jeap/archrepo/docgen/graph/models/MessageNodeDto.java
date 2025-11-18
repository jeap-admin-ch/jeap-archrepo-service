package ch.admin.bit.jeap.archrepo.docgen.graph.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageNodeDto implements NodeDto {
    private long id;
    private String messageType;
    private String variant;
    private boolean highlighted = false;

    @Override
    public String getDotId() {
        return NodeDtoType.MESSAGE.name() + "-" + id;
    }

    @Override
    public String toDot() {
        String style = highlighted ? ", style=filled, fillcolor=lightblue" : "";
        String messageNodeName = messageType.replaceAll("(Event|Command)$", "");
        String label = variant != null && !variant.isBlank()
                ? String.format("%s\\n[%s]", messageNodeName, variant)
                : messageNodeName;
        return String.format("  \"%s\" [label=\"%s\", shape=ellipse%s];", getDotId(), label, style);
    }
}