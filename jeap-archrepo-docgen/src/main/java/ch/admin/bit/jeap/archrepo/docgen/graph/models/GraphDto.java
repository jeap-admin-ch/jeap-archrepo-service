package ch.admin.bit.jeap.archrepo.docgen.graph.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphDto {
    private List<NodeDto> nodes;
    private List<EdgeDto> edges;

    public String toDot() {
        StringBuilder dot = new StringBuilder("digraph G {\n");
        dot.append("  rankdir=LR;\n");

        for (NodeDto node : nodes) {
            dot.append(node.toDot()).append("\n");
        }

        for (EdgeDto edge : edges) {
            dot.append(edge.toDot()).append("\n");
        }

        dot.append("}\n");
        return dot.toString();
    }
}