package ch.admin.bit.jeap.archrepo.test;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageContract;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageVersion;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.model.database.DatabaseSchema;
import ch.admin.bit.jeap.archrepo.model.database.Table;
import ch.admin.bit.jeap.archrepo.model.database.TableColumn;
import ch.admin.bit.jeap.archrepo.model.database.TablePrimaryKey;
import ch.admin.bit.jeap.archrepo.persistence.ArtifactIndexEntry;
import ch.admin.bit.jeap.archrepo.persistence.MessageTypeVersionDetail;
import ch.admin.bit.jeap.archrepo.persistence.MessageTypeVersionIndexEntry;
import lombok.Value;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The landscape the provider states of the docs API put the arch repo into.
 * <p>
 * <b>One landscape for every resource</b>, rather than one stub per state: the docs API is read by a single
 * consumer that walks it whole - the systems, then one system, its messages, the two artifact indexes, the
 * artifacts themselves and the message type versions - and a state that invented its own system for each of
 * those would let the resources disagree about what exists.
 * <p>
 * It is deliberately small but not degenerate: a system with an alias, two components of which one has both
 * artifacts and one has neither, an active and a deleted relation, an event with a publisher and a consumer
 * contract, and a command. What the payloads have to render is what is here; anything a consumer matches by
 * type it can match against these values.
 *
 * @see ch.admin.bit.jeap.archrepo.web.rest.model.PactProviderTestBase
 */
@UtilityClass
public class DocsApiPactStubs {

    public static final String SYSTEM = "orders";
    public static final String SYSTEM_ALIAS = "ORDERS-ALIAS";
    public static final String COMPONENT = "orders-shipping-service";
    public static final String COMPONENT_WITHOUT_ARTIFACTS = "orders-plain-service";
    public static final String CONSUMER_COMPONENT = "billing-gateway";
    public static final String EVENT_NAME = "OrdersOrderPlacedEvent";
    public static final String COMMAND_NAME = "OrdersCheckStockV2Command";
    public static final String MESSAGE_VERSION = "1.0.0";
    public static final String OPENAPI_VERSION = "1.4.2";
    public static final String DATABASE_SCHEMA_VERSION = "42";

    /**
     * The stored OpenAPI specification, served byte for byte by the content resource - so what a consumer
     * matches is a specification and not a placeholder.
     */
    public static final byte[] OPENAPI_CONTENT = ("""
            {"openapi":"3.0.3","info":{"title":"Shipping","version":"1.4.2"},\
            "servers":[{"url":"https://shipping.example.com"}],\
            "paths":{"/api/shipments/{id}":{"get":{"summary":"One shipment"}}}}""")
            .getBytes(StandardCharsets.UTF_8);

    /** A one-table schema, rendered by the same record model the importer stores. */
    public static byte[] databaseSchemaContent() {
        return databaseSchema().toJson();
    }

    /**
     * The whole landscape, for the three model resources.
     */
    public static ArchitectureModel model() {
        Team team = Team.builder()
                .name("Team Blue")
                .contactAddress("team-blue@example.com")
                .jiraLink("https://jira.example.com/projects/ORDERS")
                .confluenceLink("https://confluence.example.com/display/ORDERS")
                .build();

        BackendService component = BackendService.builder()
                .id(UUID.randomUUID())
                .name(COMPONENT)
                .description("Ships what was ordered")
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
                .description("The order management example system")
                .aliases(List.of(SYSTEM_ALIAS))
                .defaultOwner(team)
                .systemComponents(List.of(component, plainComponent))
                .build();

        RestApi restApi = RestApi.builder()
                .provider(component)
                .method("GET")
                .path("/api/shipments/{id}")
                .importer(Importer.OPEN_API)
                .build();
        system.addRestApi(restApi);

        system.addRelation(RestApiRelation.builder()
                .definingSystem(system)
                .consumerName(CONSUMER_COMPONENT)
                .restApi(restApi)
                .importer(Importer.PACT_BROKER)
                .pactUrl("https://pactbroker.example.com/pacts/shipping")
                .build());

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .messageTypeName(EVENT_NAME)
                .scope(SYSTEM)
                .topic("orders-order-event")
                .descriptorUrl("https://descriptors.example.com/order-placed.json")
                .messageVersions(List.of(messageVersion()))
                .build();
        event.addPublisherContract(MessageContract.builder()
                .componentName(COMPONENT)
                .topic("orders-order-event")
                .version(List.of(MESSAGE_VERSION))
                .build());
        event.addConsumerContract(MessageContract.builder()
                .componentName(CONSUMER_COMPONENT)
                .topic("orders-order-event")
                .version(List.of(MESSAGE_VERSION))
                .build());
        system.addEvent(event);

        Command command = Command.builder()
                .id(UUID.randomUUID())
                .messageTypeName(COMMAND_NAME)
                .scope(SYSTEM)
                .topic("orders-stock-command")
                .descriptorUrl("https://descriptors.example.com/check-stock.json")
                .messageVersions(List.of(messageVersion()))
                .build();
        command.addSenderContract(MessageContract.builder()
                .componentName(COMPONENT)
                .topic("orders-stock-command")
                .version(List.of(MESSAGE_VERSION))
                .build());
        system.addCommand(command);

        system.addRelation(EventRelation.builder()
                .definingSystem(system)
                .consumerName(CONSUMER_COMPONENT)
                .providerName(COMPONENT)
                .eventName(EVENT_NAME)
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .build());

        // Both artifacts hang on the model, because the system export carries a reference to each of them and a
        // consumer maps those references - a landscape without them would leave that mapping uncovered.
        system.addOpenApiSpec(openApiSpec(component));
        system.addDatabaseSchema(databaseSchema(component));

        return ArchitectureModel.builder()
                .systems(List.of(system))
                .teams(List.of(team))
                .openApiBaseUrl("https://archrepo.example.com/swagger-ui/index.html?url=/api/openapi/")
                .build();
    }

    /**
     * The component the artifact resources serve, with its parent system - the artifact resources check that
     * the two path segments agree, so both have to come from one landscape.
     */
    public static SystemComponent documentedComponent() {
        return model().getSystems().getFirst().findSystemComponent(COMPONENT).orElseThrow();
    }

    public static OpenApiSpec openApiSpec(SystemComponent provider) {
        return OpenApiSpec.builder()
                .provider(provider)
                .version(OPENAPI_VERSION)
                .serverUrl("https://shipping.example.com")
                .content(OPENAPI_CONTENT)
                .build();
    }

    public static SystemComponentDatabaseSchema databaseSchema(SystemComponent component) {
        return SystemComponentDatabaseSchema.builder()
                .systemComponent(component)
                .schema(databaseSchemaContent())
                .schemaVersion(DATABASE_SCHEMA_VERSION)
                .build();
    }

    /**
     * The index entry of the OpenAPI specification. Its content hash is the hash of the very bytes the content
     * resource serves, because that is what the index promises: the entity tag of an entry is byte-identical to
     * the {@code ETag} of the resource it points at, and a consumer compares the two without a request.
     */
    public static ArtifactIndexEntry openApiIndexEntry() {
        return new IndexEntry(SYSTEM, COMPONENT, OPENAPI_VERSION,
                ch.admin.bit.jeap.archrepo.metamodel.ContentHash.of(OPENAPI_CONTENT),
                ZonedDateTime.parse("2026-08-12T04:00:00Z"));
    }

    public static ArtifactIndexEntry databaseSchemaIndexEntry() {
        return new IndexEntry(SYSTEM, COMPONENT, DATABASE_SCHEMA_VERSION,
                ch.admin.bit.jeap.archrepo.metamodel.ContentHash.of(databaseSchemaContent()),
                ZonedDateTime.parse("2026-08-12T04:00:00Z"));
    }

    public static MessageTypeVersionIndexEntry messageTypeIndexEntry() {
        return new MessageTypeIndexEntry(SYSTEM, EVENT_NAME, "EVENT", MESSAGE_VERSION);
    }

    /**
     * One version of the event, with both schemas: the key schema is optional in the payload, so a state that
     * left it out would let a consumer's mapping of it go unverified.
     */
    public static MessageTypeVersionDetail messageTypeVersion() {
        return new MessageTypeVersionDetail(EVENT_NAME, MESSAGE_VERSION, "BACKWARD", null,
                "OrdersOrderPlacedEventKey.avdl",
                "https://schemas.example.com/orders/OrdersOrderPlacedEventKey.avdl",
                """
                        {"type":"record","name":"OrdersOrderPlacedEventKey","fields":[\
                        {"name":"orderId","type":"string"}]}""",
                "OrdersOrderPlacedEvent.avdl",
                "https://schemas.example.com/orders/OrdersOrderPlacedEvent.avdl",
                """
                        {"type":"record","name":"OrdersOrderPlacedEvent","fields":[\
                        {"name":"placedAt","type":"string"}]}""");
    }

    private static MessageVersion messageVersion() {
        return MessageVersion.builder()
                .version(MESSAGE_VERSION)
                .valueSchemaName("OrdersOrderPlacedEvent.avdl")
                .valueSchemaUrl("https://schemas.example.com/orders/OrdersOrderPlacedEvent.avdl")
                .valueSchemaResolved("{\"type\":\"record\"}")
                .build();
    }

    private static DatabaseSchema databaseSchema() {
        Table shipment = Table.builder()
                .name("shipment")
                .columns(List.of(
                        new TableColumn("shipment_id", "uuid", false),
                        new TableColumn("placed_at", "timestamptz", true)))
                .primaryKey(new TablePrimaryKey("pk_shipment", List.of("shipment_id")))
                .build();
        return DatabaseSchema.builder()
                .name("orders")
                .version(DATABASE_SCHEMA_VERSION)
                .tables(List.of(shipment))
                .build();
    }

    @Value
    private static class IndexEntry implements ArtifactIndexEntry {
        String system;
        String component;
        String version;
        String contentHash;
        ZonedDateTime lastModifiedAt;
    }

    @Value
    private static class MessageTypeIndexEntry implements MessageTypeVersionIndexEntry {
        String system;
        String message;
        String kind;
        String version;
    }
}
