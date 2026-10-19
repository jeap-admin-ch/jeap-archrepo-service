package ch.admin.bit.jeap.archrepo.docgen.plantuml;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;
import lombok.Builder;
import lombok.Value;

/**
 * A directed edge / arrow from a source to a target. One arrow per relation type (REST call, event, command, ...) is
 * shown from a source to a target to limit the amount of visible lines on a diagram.
 */
@Value
@Builder
class PlantUmlRelationEdge {
    String source;
    String target;
    RelationType type;

    String getSource() {
        return PlantUmlComponent.componentName(source);
    }

    String getTarget() {
        return PlantUmlComponent.componentName(target);
    }
}
