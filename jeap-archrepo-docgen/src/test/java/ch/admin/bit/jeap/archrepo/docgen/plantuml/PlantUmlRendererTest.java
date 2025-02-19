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
}
