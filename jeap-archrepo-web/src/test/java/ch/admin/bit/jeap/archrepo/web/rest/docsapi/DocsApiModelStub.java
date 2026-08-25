package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageContract;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageVersion;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationStatus;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A landscape with everything the docs API has to render: two systems, an alias, a component with and one without
 * artifacts, an active and a deleted relation, an event and a command.
 */
final class DocsApiModelStub {

    static final String SYSTEM = "wvs";
    static final String SYSTEM_ALIAS = "WVS-ALIAS";
    static final String OTHER_SYSTEM = "zoll";
    static final String COMPONENT = "wvs-foo-bar-service";
    static final String COMPONENT_WITHOUT_ARTIFACTS = "wvs-plain-service";
    static final String OTHER_COMPONENT = "zoll-gateway";
    static final byte[] OPEN_API_CONTENT = "{\"openapi\":\"3.0.0\"}".getBytes(StandardCharsets.UTF_8);
    static final String EVENT_NAME = "WvsDeclarationAcceptedEvent";
    static final String COMMAND_NAME = "WvsCheckNctsReferabilityV2Command";

    private DocsApiModelStub() {
    }

    static final String OPEN_API_BASE_URL =
            "https://archrepo.example.com/archrepo-service/swagger-ui/index.html?url=/archrepo-service/api/openapi/";

    static ArchitectureModel create() {
        return create(false);
    }

    /**
     * The same landscape, with every nested collection built in the opposite order - the payload must not notice.
     */
    static ArchitectureModel createWithNestedCollectionsReversed() {
        return create(true);
    }

    private static ArchitectureModel create(boolean reverseNested) {
        Team team = Team.builder()
                .name("Team Blue")
                .contactAddress("team-blue@example.com")
                .jiraLink("https://jira.example.com/projects/WVS")
                .confluenceLink("https://confluence.example.com/display/WVS")
                .build();

        BackendService component = BackendService.builder()
                .id(UUID.randomUUID())
                .name(COMPONENT)
                .description("Handles foo and bar")
                .importer(Importer.DEPLOYMENT_LOG)
                .build();
        component.setLastSeenFromDate(ZonedDateTime.parse("2026-08-12T04:00:00Z"));

        BackendService plainComponent = BackendService.builder()
                .id(UUID.randomUUID())
                .name(COMPONENT_WITHOUT_ARTIFACTS)
                .importer(Importer.GRAFANA)
                .build();

        System system = System.builder()
                .name(SYSTEM)
                .description("Warenverkehrssystem")
                .aliases(List.of(SYSTEM_ALIAS))
                .defaultOwner(team)
                .systemComponents(reverseNested
                        ? List.of(plainComponent, component)
                        : List.of(component, plainComponent))
                .build();

        BackendService otherComponent = BackendService.builder()
                .id(UUID.randomUUID())
                .name(OTHER_COMPONENT)
                .importer(Importer.GRAFANA)
                .build();
        System otherSystem = System.builder()
                .name(OTHER_SYSTEM)
                .systemComponents(List.of(otherComponent))
                .build();

        RestApi restApi = RestApi.builder()
                .provider(component)
                .method("GET")
                .path("/api/foo/{id}")
                .importer(Importer.OPEN_API)
                .build();
        system.addRestApi(restApi);

        system.addRelation(RestApiRelation.builder()
                .definingSystem(system)
                .consumerName(OTHER_COMPONENT)
                .restApi(restApi)
                .importer(Importer.PACT_BROKER)
                .pactUrl("https://pactbroker.example.com/pacts/foo")
                .build());

        // A deleted relation must not appear in the payload
        RestApiRelation deleted = RestApiRelation.builder()
                .definingSystem(system)
                .consumerName(OTHER_COMPONENT)
                .restApi(restApi)
                .importer(Importer.PACT_BROKER)
                .build();
        ReflectionTestUtils.setField(deleted, "status", RelationStatus.DELETED);
        system.addRelation(deleted);

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .messageTypeName(EVENT_NAME)
                .scope(SYSTEM)
                .topic("wvs-declaration-event")
                .descriptorUrl("https://descriptors.example.com/declaration.json")
                .messageVersions(reverseNested
                        ? List.of(messageVersion("2.0.0"), messageVersion("1.0.0"))
                        : List.of(messageVersion("1.0.0"), messageVersion("2.0.0")))
                .build();
        if (reverseNested) {
            event.addConsumerContract(MessageContract.builder()
                    .componentName(OTHER_COMPONENT).topic("wvs-declaration-event").version(List.of("1.0.0", "2.0.0")).build());
            event.addPublisherContract(MessageContract.builder()
                    .componentName(COMPONENT).topic("wvs-declaration-event").version(List.of("2.0.0")).build());
        } else {
            event.addPublisherContract(MessageContract.builder()
                    .componentName(COMPONENT).topic("wvs-declaration-event").version(List.of("2.0.0")).build());
            event.addConsumerContract(MessageContract.builder()
                    .componentName(OTHER_COMPONENT).topic("wvs-declaration-event").version(List.of("1.0.0", "2.0.0")).build());
        }
        system.addEvent(event);

        Command command = Command.builder()
                .id(UUID.randomUUID())
                .messageTypeName(COMMAND_NAME)
                .scope(SYSTEM)
                .topic("wvs-ncts-command")
                .descriptorUrl("https://descriptors.example.com/ncts.json")
                .messageVersions(List.of(messageVersion("1.0.0")))
                .build();
        command.addSenderContract(MessageContract.builder()
                .componentName(COMPONENT).topic("wvs-ncts-command").version(List.of("1.0.0")).build());
        system.addCommand(command);

        system.addRelation(EventRelation.builder()
                .definingSystem(system)
                .consumerName(OTHER_COMPONENT)
                .providerName(COMPONENT)
                .eventName(EVENT_NAME)
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .build());

        OpenApiSpec spec = OpenApiSpec.builder()
                .provider(component)
                .version("1.4.2")
                .serverUrl("https://foo-bar.example.com")
                .content(OPEN_API_CONTENT)
                .build();
        system.addOpenApiSpec(spec);

        return ArchitectureModel.builder()
                .systems(List.of(system, otherSystem))
                .teams(List.of(team))
                .openApiBaseUrl(OPEN_API_BASE_URL)
                .build();
    }

    private static MessageVersion messageVersion(String version) {
        return MessageVersion.builder()
                .version(version)
                .valueSchemaName("schema")
                .valueSchemaUrl("https://schemas.example.com/" + version)
                .valueSchemaResolved("{}")
                .build();
    }
}
