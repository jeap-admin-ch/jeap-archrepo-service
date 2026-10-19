package ch.admin.bit.jeap.archrepo.docgen.plantuml;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlantUmlRelationTest {

    @Test
    void label() {
        PlantUmlRelationEdge line = PlantUmlRelationEdge.builder()
                .source("source")
                .target("target")
                .type(RelationType.COMMAND_RELATION)
                .build();
        PlantUmlRelation relation1 = new PlantUmlRelation(line);
        PlantUmlRelation relation3 = new PlantUmlRelation(line);
        PlantUmlRelation relation10 = new PlantUmlRelation(line);

        relation1.addLabel("label");
        IntStream.range(0, 3).forEach(i -> relation3.addLabel("label " + i));
        IntStream.range(0, 10).forEach(i -> relation10.addLabel("label " + i));

        assertEquals("label", relation1.label());
        assertEquals("label 0\\llabel 1\\llabel 2", relation3.label());
        assertEquals("10 Command Relations", relation10.label());
    }
}
