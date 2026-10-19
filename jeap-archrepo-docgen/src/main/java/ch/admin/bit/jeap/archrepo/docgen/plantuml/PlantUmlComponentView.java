package ch.admin.bit.jeap.archrepo.docgen.plantuml;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

class PlantUmlComponentView {

    private static final String START_UML = "@startuml\n";
    private static final String END_UML = "@enduml";
    private static final String LAYOUT = "left to right direction\n";

    // Using sets/maps with predictable ordering to ensure stable component/relation ordering in the view
    private final Set<PlantUmlComponent> components = new LinkedHashSet<>();
    private final Map<PlantUmlRelationEdge, PlantUmlRelation> relations = new LinkedHashMap<>();

    PlantUmlComponent addFocusedComponent(String label) {
        PlantUmlComponent component = PlantUmlComponent.focused(label);
        components.add(component);
        return component;
    }

    PlantUmlComponent addComponent(String label) {
        PlantUmlComponent component = PlantUmlComponent.of(label);
        components.add(component);
        return component;
    }

    void addRelation(String source, String target, RelationType type, String label) {
        PlantUmlRelationEdge line = PlantUmlRelationEdge.builder()
                .source(source)
                .target(target)
                .type(type)
                .build();
        PlantUmlRelation plantUmlRelation = relations.computeIfAbsent(line, key -> new PlantUmlRelation(line));
        plantUmlRelation.addLabel(label);
    }

    String render() {
        StringBuilder uml = new StringBuilder(START_UML);
        uml.append(LAYOUT);
        components.forEach(component -> component.render(uml));
        relations.forEach((line, relation) -> relation.render(uml));
        uml.append(END_UML);
        return uml.toString();
    }
}
