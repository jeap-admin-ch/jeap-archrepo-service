package ch.admin.bit.jeap.archrepo.docgen.plantuml;

import ch.admin.bit.jeap.archrepo.docgen.ComponentContext;
import ch.admin.bit.jeap.archrepo.docgen.SystemContext;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.relation.CommandRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.model.database.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlantUmlRendererTest {

    private ComponentContext componentContext;
    private PlantUmlRenderer plantUmlRenderer;
    private SystemContext systemContext;

    @Test
    void renderComponentContextView() {
        String uml = plantUmlRenderer.renderComponentContextView(componentContext);

        assertEquals("""
                @startuml
                left to right direction
                component "component1-service" as component1_service [[./component1-service]]  #Gold
                component "component2-service" as component2_service [[./component2-service]]\s
                component "other-service" as other_service [[./other-service]]\s
                other_service -[#green,dashed]-> component1_service : "OtherEvent2"
                component1_service -[#green,dashed]-> component2_service : "MyEvent1\\lMyEvent2"
                component1_service -[#green,dashed]-> other_service : "OtherEvent1"
                component1_service -[#blue,dashed]-> component2_service : "MyCommand"
                component2_service -[#blue]-> component1_service : "GET /api/outgoing"
                @enduml""", uml);
    }

    @Test
    void renderSystemContextView() {
        String uml = plantUmlRenderer.renderSystemContextView(systemContext);

        assertEquals("""
                @startuml
                left to right direction
                component "system" as system  #Gold
                component "otherSystem" as otherSystem\s
                otherSystem -[#green,dashed]-> system : "OtherEvent2"
                system -[#green,dashed]-> otherSystem : "OtherEvent1"
                @enduml""", uml);
    }

    @Test
    void renderDatabaseSchema() {
        DatabaseSchema dbSchema = createDatabaseSchema();
        PlantUmlDbSchemaRenderer dbRenderer = new PlantUmlDbSchemaRenderer();

        String uml = dbRenderer.renderDbSchema(dbSchema);

        assertEquals("""
                    @startuml
                    title test-schema in Komponentenversion 1.2.3
                    !theme mars
                    skinparam linetype curved
                    
                    entity "table_foo" {
                      * col_a : text <<PK>>
                        --
                        col_b : bytea
                      * col_c : text
                    }
                    
                    entity "table_bar" {
                        ref_col_a : text <<FK>>
                      * col_d : text
                    }
                    
                    
                    "table_bar" }o--|| "table_foo" : ref_col_a
                    @enduml
                    """, uml);
    }

    @BeforeEach
    void beforeEach() {
        plantUmlRenderer = new PlantUmlRenderer();
        BackendService component1 = BackendService.builder().name("component1-service").build();
        BackendService component2 = BackendService.builder().name("component2-service").build();
        System system = System.builder()
                .name("system")
                .systemComponents(List.of(component1, component2))
                .build();
        BackendService otherComponent = BackendService.builder().name("other-service").build();
        System otherSystem = System.builder()
                .name("otherSystem")
                .systemComponents(List.of(otherComponent))
                .build();
        ArchitectureModel model = ArchitectureModel.builder()
                .systems(List.of(system, otherSystem))
                .build();

        RestApi counterpartRestApi = RestApi.builder()
                .provider(component1)
                .method("GET")
                .path("/api/outgoing")
                .build();
        RestApiRelation restRelation = RestApiRelation.builder()
                .consumerName(component2.getName())
                .restApi(counterpartRestApi)
                .pactUrl("http://pact_incoming")
                .lastSeen(ZonedDateTime.now())
                .build();
        EventRelation eventRelation1 = EventRelation.builder()
                .consumerName(component2.getName())
                .providerName(component1.getName())
                .eventName("MyEvent1")
                .build();
        EventRelation eventRelation2 = EventRelation.builder()
                .consumerName(component2.getName())
                .providerName(component1.getName())
                .eventName("MyEvent2")
                .build();
        CommandRelation commandRelation = CommandRelation.builder()
                .consumerName(component2.getName())
                .providerName(component1.getName())
                .commandName("MyCommand")
                .build();
        system.addRelation(restRelation);
        system.addRelation(eventRelation1);
        system.addRelation(eventRelation2);
        system.addRelation(commandRelation);

        EventRelation otherSystemOutgoingRelation = EventRelation.builder()
                .consumerName(otherComponent.getName())
                .providerName(component1.getName())
                .eventName("OtherEvent1")
                .build();
        EventRelation otherSystemIncomingRelation = EventRelation.builder()
                .consumerName(component1.getName())
                .providerName(otherComponent.getName())
                .eventName("OtherEvent2")
                .build();
        otherSystem.addRelation(otherSystemOutgoingRelation);
        system.addRelation(otherSystemIncomingRelation);

        componentContext = ComponentContext.of(model, component1);
        systemContext = SystemContext.of(model, system);
    }

    private DatabaseSchema createDatabaseSchema() {
        Table tableA = Table.builder()
                .name("table_foo")
                .columns(List.of(
                        new TableColumn("col_b", "bytea", false),
                        new TableColumn("col_a", "text", false),
                        new TableColumn("col_c", "text", true)))
                .primaryKey(new TablePrimaryKey("pk_foo", List.of("col_a")))
                .build();
        Table tableB = Table.builder()
                .name("table_bar")
                .columns(List.of(
                        new TableColumn("ref_col_a", "text", false),
                        new TableColumn("col_d", "text", true)))
                .foreignKeys(List.of(
                        TableForeignKey.builder()
                                .name("fk_foo_bar")
                                .columnNames(List.of("ref_col_a"))
                                .referencedTableName("table_foo")
                                .referencedColumnNames(List.of("col_a"))
                                .build()))
                .build();
        return DatabaseSchema.builder()
                .name("test-schema")
                .version("1.2.3")
                .tables(List.of(tableA, tableB))
                .build();
    }

}
