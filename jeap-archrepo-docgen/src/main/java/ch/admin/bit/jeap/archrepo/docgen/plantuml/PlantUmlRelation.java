package ch.admin.bit.jeap.archrepo.docgen.plantuml;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
class PlantUmlRelation {

    private static final String EVENT_ARROW = " -[#green,dashed]-> ";
    private static final String COMMAND_ARROW = " -[#blue,dashed]-> ";
    private static final String SYNC_ARROW = " -[#blue]-> ";

    PlantUmlRelationEdge edge;
    List<String> labels = new ArrayList<>();

    PlantUmlRelation(PlantUmlRelationEdge edge) {
        this.edge = edge;
    }

    void addLabel(String label) {
        labels.add(label);
    }

    void render(StringBuilder uml) {
        uml.append(edge.getSource())
                .append(arrow(edge.getType()))
                .append(edge.getTarget())
                .append(" : \"")
                .append(label())
                .append("\"\n");
    }

    /**
     * A multi-line label for the relation. Too many label lines for a single line are hard to read and layout, so
     * there is a limit of 5 label lines, after that only the amount of relations are shown in the label.
     */
    String label() {
        int labelCount = labels.size();
        if (labelCount > 5) {
            return labelCount + " " + edge.getType().getLabel() + "s";
        }
        // \l = left align, makes it easier to read multi-line labels in big diagrams
        return String.join("\\l", labels);
    }

    private static String arrow(RelationType type) {
        return switch (type) {
            case COMMAND_RELATION -> COMMAND_ARROW;
            case EVENT_RELATION -> EVENT_ARROW;
            default -> SYNC_ARROW;
        };
    }
}
