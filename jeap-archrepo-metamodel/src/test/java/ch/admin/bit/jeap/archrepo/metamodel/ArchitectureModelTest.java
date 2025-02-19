package ch.admin.bit.jeap.archrepo.metamodel;

import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.relation.CommandRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ArchitectureModelTest {

    private ArchitectureModel model;
    private System system1;
    private System system2;
    private Frontend frontend;
    private BackendService backend;
    private BackendService backend2;
    private System otherSystem;
    private final BackendService otherBackendService1 = BackendService.builder().name("other-backend-service-1").build();
    private final BackendService otherBackendService2 = BackendService.builder().name("other-backend-service-2").build();

    @Test
    void findSystem() {
        Optional<System> exactMatch = model.findSystem("system1");
        Optional<System> ignoredCase = model.findSystem("System1");
        Optional<System> inexistant = model.findSystem("foo");
        Optional<System> foundViaAlias = model.findSystem("system1alias");

        assertTrue(exactMatch.isPresent());
        assertSame(system1, exactMatch.get());
        assertTrue(ignoredCase.isPresent());
        assertSame(system1, ignoredCase.get());
        assertTrue(inexistant.isEmpty());
        assertTrue(foundViaAlias.isPresent());
        assertSame(system1, foundViaAlias.get());
    }

    @Test
    void findSystemComponent() {
        Optional<SystemComponent> exactMatch = model.findSystemComponent(frontend.getName());
        Optional<SystemComponent> ignoredCase = model.findSystemComponent(frontend.getName().toUpperCase());
        Optional<SystemComponent> inexistant = model.findSystemComponent("foo");

        assertTrue(exactMatch.isPresent());
        assertSame(frontend, exactMatch.get());
        assertTrue(ignoredCase.isPresent());
        assertSame(frontend, ignoredCase.get());
        assertTrue(inexistant.isEmpty());
    }

    @Test
    void removeAllByImporter() {
        SystemComponent additionalComponent = BackendService.builder()
                .name("additional")
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .build();
        system1.addSystemComponent(additionalComponent);
        assertTrue(model.getAllSystemComponentsByImporter(Importer.MESSAGE_TYPE_REGISTRY).contains(additionalComponent));

        model.removeAllByImporter(Importer.MESSAGE_TYPE_REGISTRY);

        assertFalse(model.getAllSystemComponentsByImporter(Importer.MESSAGE_TYPE_REGISTRY).contains(additionalComponent));
    }

    @Test
    void builderSetsParent() {
        assertSame(system1, backend.getParent());
        assertSame(system1, backend2.getParent());
        assertSame(system1, frontend.getParent());
    }

    @Test
    void removingSystemComponentsRemovesReferencingRelationsAndRestApis() {
        // given
        RestApiRelation removedComponentRestProviderRelation = RestApiRelation.builder()
                .consumerName(frontend.getName())
                .restApi(createRestApi(backend, "PUT", "/api/foo"))
                .pactUrl("http://pact_consumer")
                .lastSeen(ZonedDateTime.now())
                .build();
        EventRelation removedComponentEventConsumerRelation = EventRelation.builder()
                .eventName("MyEvent")
                .consumerName(backend.getName())
                .providerName(otherBackendService1.getName())
                .build();
        EventRelation removedComponentEventProducerRelation = EventRelation.builder()
                .eventName("MyEvent")
                .consumerName(otherBackendService1.getName())
                .providerName(backend.getName())
                .build();
        EventRelation notRemovedRelation1 = EventRelation.builder()
                .eventName("MyEvent")
                .consumerName(frontend.getName())
                .providerName(frontend.getName())
                .build();
        EventRelation notRemovedRelation2 = EventRelation.builder()
                .eventName("MyEvent")
                .consumerName(otherBackendService1.getName())
                .providerName(otherBackendService2.getName())
                .build();
        system1.addRelation(removedComponentRestProviderRelation);
        system1.addRelation(removedComponentEventConsumerRelation);
        system1.addRelation(notRemovedRelation1);
        otherSystem.addRelation(removedComponentEventProducerRelation);
        otherSystem.addRelation(notRemovedRelation2);

        createRestApi(backend, "PUT", "/api/foo");
        createRestApi(backend2, "PUT", "/api/foo");

        // when
        system1.removeSystemComponent(backend);

        // then
        assertFalse(system1.getRelations().contains(removedComponentRestProviderRelation));
        assertFalse(system1.getRelations().contains(removedComponentEventConsumerRelation));
        assertFalse(otherSystem.getRelations().contains(removedComponentEventProducerRelation));
        assertTrue(otherSystem.getRelations().contains(notRemovedRelation2));
        assertFalse(system1.getRestApis().stream().anyMatch(api -> api.getProvider() == backend));
        assertTrue(system1.getRestApis().stream().anyMatch(api -> api.getProvider() == backend2));
    }

    @Test
    void removingEventAndCommandRemovesReferencingRelations() {
        // given
        Event event = Event.builder()
                .scope("public")
                .messageVersions(List.of())
                .messageTypeName("EventName")
                .descriptorUrl("link")
                .publisherContracts(List.of())
                .consumerContracts(List.of())
                .build();
        Command command = Command.builder()
                .scope("public")
                .messageVersions(List.of())
                .messageTypeName("CommandName")
                .descriptorUrl("link")
                .senderContracts(List.of())
                .receiverContracts(List.of())
                .build();
        EventRelation eventRelationForDeletedEvent = EventRelation.builder()
                .eventName(event.getMessageTypeName())
                .build();
        CommandRelation commandRelationForDeletedCommand = CommandRelation.builder()
                .commandName(command.getMessageTypeName())
                .build();
        EventRelation remainingEventRelation = EventRelation.builder()
                .eventName("not-removed-event")
                .build();
        CommandRelation remainingCommandRelation = CommandRelation.builder()
                .commandName("not-removed-command")
                .build();
        system1.addRelation(eventRelationForDeletedEvent);
        system1.addRelation(remainingEventRelation);
        otherSystem.addRelation(commandRelationForDeletedCommand);
        otherSystem.addRelation(remainingCommandRelation);

        // when
        system1.removeEvent(event);
        system1.removeCommand(command);

        // then
        assertFalse(system1.getRelations().contains(eventRelationForDeletedEvent));
        assertFalse(otherSystem.getRelations().contains(commandRelationForDeletedCommand));
        assertTrue(system1.getRelations().contains(remainingEventRelation));
        assertTrue(otherSystem.getRelations().contains(remainingCommandRelation));
    }

    @Test
    void cleanup() {
        //given
        RestApiRelation notRemovedRestApiRelationBecauseNotOld = RestApiRelation.builder()
                .consumerName(frontend.getName())
                .restApi(createRestApi(backend, "PUT", "/api/foo1"))
                .pactUrl("http://pact_consumer")
                .lastSeen(ZonedDateTime.now())
                .importer(Importer.GRAFANA)
                .build();

        // Create an operation that is imported and unused, should be removed by cleanup()
        createRestApi(backend, "GET", "/api/foo1");

        RestApiRelation notRemovedRestApiRelationBecauseNoImporter = RestApiRelation.builder()
                .consumerName(frontend.getName())
                .restApi(createRestApi(backend, "PUT", "/api/foo2"))
                .pactUrl("http://pact_consumer")
                .lastSeen(ZonedDateTime.now())
                .build();

        RestApiRelation removedRestApiRelation = RestApiRelation.builder()
                .consumerName(frontend.getName())
                .restApi(createRestApi(backend, "GET", "/api/foo"))
                .pactUrl("http://pact_consumer")
                .lastSeen(ZonedDateTime.now().minusMonths(4))
                .importer(Importer.GRAFANA)
                .build();

        system1.addRelation(notRemovedRestApiRelationBecauseNotOld);
        system1.addRelation(notRemovedRestApiRelationBecauseNoImporter);
        system1.addRelation(removedRestApiRelation);

        createRestApi(backend, "GET", "/api/foo");

        //when
        model.cleanup();

        //then
        assertTrue(system1.getRelations().contains(notRemovedRestApiRelationBecauseNotOld));
        assertTrue(system1.getRelations().contains(notRemovedRestApiRelationBecauseNoImporter));
        assertFalse(system1.getRelations().contains(removedRestApiRelation));
        assertTrue(system1.getRestApis().stream()
                .noneMatch(res -> "/api/foo".equals(res.getPath())), "/api/foo is no longer used and has been removed");
        assertTrue(system1.getRestApis().stream()
                .anyMatch(res -> "/api/foo1".equals(res.getPath())), "/api/foo1 is still used and has not been removed");
        assertTrue(system1.getRestApis().stream()
                .anyMatch(op -> "PUT".equals(op.getMethod()) && "/api/foo1".equals(op.getPath())), "/api/foo1 PUT is still used and has not been removed");
        assertTrue(system1.getRestApis().stream()
                .noneMatch(op -> "GET".equals(op.getMethod()) && "/api/foo1".equals(op.getPath())), "/api/foo1 PUT is still used and has not been removed");
        assertTrue(system1.getRestApis().stream()
                .anyMatch(res -> "/api/foo2".equals(res.getPath())), "/api/foo2 is still used and has not been removed");
    }

    private RestApi createRestApi(SystemComponent provider, String method, String path) {
        RestApi restApi = RestApi.builder()
                .provider(provider)
                .method(method)
                .path(path)
                .importer(Importer.GRAFANA)
                .build();
        provider.getParent().addRestApi(restApi);
        return restApi;
    }

    @Test
    void getAllRestApiRelations() {
        // given
        RestApi restApi = createRestApi(backend, "PUT", "/api/foo");


        RestApiRelation restApiRelation1 = RestApiRelation.builder()
                .consumerName(frontend.getName())
                .restApi(restApi)
                .pactUrl("http://pact_consumer")
                .lastSeen(ZonedDateTime.now())
                .build();

        RestApiRelation restApiRelation2 = RestApiRelation.builder()
                .consumerName(frontend.getName())
                .restApi(createRestApi(backend, "GET", "/api/foo"))
                .pactUrl("http://pact_consumer")
                .lastSeen(ZonedDateTime.now().minusMonths(4))
                .importer(Importer.GRAFANA)
                .build();

        RestApiRelation restApiRelation3 = RestApiRelation.builder()
                .consumerName(frontend.getName())
                .restApi(createRestApi(backend, "PUT", "/api/foo/bar"))
                .lastSeen(ZonedDateTime.now().minusMonths(4))
                .importer(Importer.GRAFANA)
                .build();

        RestApiRelation restApiRelation4 = RestApiRelation.builder()
                .consumerName(frontend.getName())
                .restApi(restApi)
                .lastSeen(ZonedDateTime.now())
                .build();

        RestApiRelation restApiRelationDeleted = RestApiRelation.builder()
                .consumerName(frontend.getName())
                .restApi(createRestApi(backend, "PUT", "/api/foo/bar/old"))
                .lastSeen(ZonedDateTime.now().minusMonths(4))
                .importer(Importer.GRAFANA)
                .build();
        restApiRelationDeleted.markDeleted();

        CommandRelation commandRelationForDeletedCommand = CommandRelation.builder()
                .commandName("commandName")
                .build();

        system1.addRelation(restApiRelation1);
        system2.addRelation(restApiRelation3);
        otherSystem.addRelation(restApiRelation2);
        system2.addRelation(restApiRelation4);
        system2.addRelation(restApiRelationDeleted);
        system1.addRelation(commandRelationForDeletedCommand);

        // when
        List<RestApiRelation> allRestApiRelations = model.getRestApiRelationsWithoutPact();

        // then
        assertThat(allRestApiRelations).hasSize(2);

    }

    @Test
    void getRestApiForSystemComponent() {
        Optional<String> restApiForSystemComponent = model.getRestApiForSystemComponent(backend2);
        assertThat(restApiForSystemComponent)
                .isPresent()
                .contains("base-url/system1/backend2");
    }

    @Test
    void getSystemComponentsWithoutOpenApiSpec() {
        List<String> list = model.getSystemComponentsWithoutOpenApiSpec();
        assertThat(list)
                .hasSize(4)
                .containsExactlyInAnyOrder("backend", "scs", "other-backend-service-1", "other-backend-service-2");
    }

    @BeforeEach
    void buildModel() {
        frontend = Frontend.builder().name("frontend").build();
        backend = BackendService.builder().name("backend").build();
        backend2 = BackendService.builder().name("backend2").build();
        MobileApp mobileApp = MobileApp.builder().name("app").build();
        SelfContainedSystem scs = SelfContainedSystem.builder().name("scs").build();
        SelfContainedSystem scs2 = SelfContainedSystem.builder().name("scs2").build();
        UnknownSystemComponent unknown = UnknownSystemComponent.builder().name("unknown").build();
        system1 = System.builder()
                .name("system1")
                .aliases(List.of("system1alias"))
                .systemComponents(List.of(frontend, backend, backend2, mobileApp, scs, unknown, scs2))
                .build();
        system1.addOpenApiSpec(OpenApiSpec.builder().provider(backend2).content("test".getBytes(StandardCharsets.UTF_8)).build());
        system1.addOpenApiSpec(OpenApiSpec.builder().provider(scs2).content("scs2".getBytes(StandardCharsets.UTF_8)).build());
        system2 = System.builder()
                .name("system2")
                .build();
        otherSystem = System.builder()
                .name("otherSystem")
                .systemComponents(List.of(otherBackendService1, otherBackendService2))
                .build();
        model = ArchitectureModel.builder()
                .systems(List.of(system1, system2, otherSystem))
                .openApiBaseUrl("base-url/")
                .build();
    }
}
