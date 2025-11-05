package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.plantuml.PlantUmlRenderer;
import ch.admin.bit.jeap.archrepo.docgen.plantuml.RenderedDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageContract;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageVersion;
import ch.admin.bit.jeap.archrepo.metamodel.relation.CommandRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.Frontend;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.model.database.DatabaseSchema;
import ch.admin.bit.jeap.archrepo.model.database.Table;
import ch.admin.bit.jeap.archrepo.model.database.TableColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@ExtendWith(SpringExtension.class)
class TemplateRendererTest {

    private static final Pattern PLANTUML_SOURCE_PATTERN = Pattern.compile("(@startuml.*?@enduml)|( ac:macro-id=\".*?\")", Pattern.DOTALL);

    @Autowired
    ApplicationContext applicationContext;

    private TemplateRenderer templateRenderer;

    private static void assertContent(String expectationResourceName, String actualContentWithUml) throws IOException {
        // ignore UML in comparisons to make the test more self-contained - UML source code is tested in separate unit tests
        String actualContent = PLANTUML_SOURCE_PATTERN.matcher(actualContentWithUml).replaceAll("");
        assertThat(actualContent)
                .isEqualToIgnoringWhitespace(loadExpectation(expectationResourceName));
    }

    private static String loadExpectation(String name) throws IOException {
        try (InputStream stream = new ClassPathResource(name).getInputStream()) {
            return StreamUtils.copyToString(stream, StandardCharsets.UTF_8).trim();
        }
    }

    private static ArchitectureModel buildModel(System system) {
        return ArchitectureModel.builder()
                .systems(List.of(system))
                .openApiBaseUrl("https://base-url/")
                .build();
    }

    @Test
    void renderSystemPage() throws IOException {
        System system = System.builder()
                .name("System")
                .description("Description")
                .confluenceLink("https://link")
                .systemComponents(List.of(Frontend.builder()
                        .name("test-ui")
                        .description("desc")
                        .ownedBy(Team.builder().name("Testteam").build())
                        .importer(Importer.GRAFANA)
                        .build()))
                .build();

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderSystemPage(model, system, "graph-System.png");

        assertContent("system.expected", content);
    }

    @Test
    void renderSystemPageWithEvent() throws IOException {
        SystemComponent systemComponent = BackendService.builder()
                .description("Description")
                .name("Component")
                .build();
        SystemComponent otherComponent = BackendService.builder()
                .name("OtherComponent")
                .build();

        Event event = Event.builder()
                .scope("public")
                .messageVersions(List.of())
                .messageTypeName("EventName")
                .descriptorUrl("link")
                .publisherContracts(List.of())
                .consumerContracts(List.of())
                .build();

        EventRelation relation1 = EventRelation.builder()
                .consumerName(otherComponent.getName())
                .providerName(systemComponent.getName())
                .eventName(event.getMessageTypeName())
                .build();
        EventRelation relation2 = EventRelation.builder()
                .consumerName(systemComponent.getName())
                .providerName(systemComponent.getName())
                .eventName(event.getMessageTypeName())
                .build();

        System system = System.builder()
                .name("System")
                .description("Description")
                .confluenceLink("https://link")
                .build();
        system.addSystemComponent(systemComponent);
        system.addSystemComponent(otherComponent);
        system.addEvent(event);
        system.addRelation(relation1);
        system.addRelation(relation2);

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderSystemPage(model, system, "graph-System.png");
        assertContent("systemwithevent.expected", content);
    }

    @Test
    void renderSystemPageWithCommand() throws IOException {
        SystemComponent systemComponent = BackendService.builder()
                .description("Description")
                .name("Component")
                .build();
        SystemComponent otherComponent = BackendService.builder()
                .name("OtherComponent")
                .build();

        Command command = Command.builder()
                .scope("public")
                .messageVersions(List.of())
                .messageTypeName("CommandName")
                .descriptorUrl("link")
                .senderContracts(List.of())
                .receiverContracts(List.of())
                .build();

        CommandRelation relation1 = CommandRelation.builder()
                .consumerName(otherComponent.getName())
                .providerName(systemComponent.getName())
                .commandName(command.getMessageTypeName())
                .build();
        CommandRelation relation2 = CommandRelation.builder()
                .consumerName(systemComponent.getName())
                .providerName(systemComponent.getName())
                .commandName(command.getMessageTypeName())
                .build();

        System system = System.builder()
                .name("System")
                .description("Description")
                .confluenceLink("https://link")
                .build();
        system.addSystemComponent(systemComponent);
        system.addSystemComponent(otherComponent);
        system.addCommand(command);
        system.addRelation(relation1);
        system.addRelation(relation2);

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderSystemPage(model, system, "graph-System.png");
        assertContent("systemwithcommand.expected", content);
    }

    @Test
    void renderSystemPageWithEventWithNullConsumer() throws IOException {
        SystemComponent provider = BackendService.builder()
                .name("Provider")
                .build();

        Event event = Event.builder()
                .scope("public")
                .messageVersions(List.of())
                .description("EventDesciption")
                .messageTypeName("ProvidedEventName")
                .descriptorUrl("link")
                .documentationUrl("linkdoc")
                .topic("ConsumedTopic")
                .publisherContracts(List.of())
                .consumerContracts(List.of())
                .build();

        EventRelation relation = EventRelation.builder()
                .consumerName(null)
                .providerName(provider.getName())
                .eventName(event.getMessageTypeName())
                .build();

        System system = System.builder()
                .name("System")
                .description("Description")
                .build();
        system.addEvent(event);
        system.addSystemComponent(provider);
        system.addRelation(relation);

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderSystemPage(model, system, "graph-System.png");
        assertContent("systemwithnullevent.expected", content);
    }

    @Test
    void renderSystemComponentPage() throws IOException {
        System system = System.builder()
                .name("System")
                .description("Description")
                .build();
        SystemComponent systemComponent = BackendService.builder()
                .description("Description")
                .name("Component")
                .parent(system)
                .build();

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderComponentPage(model, systemComponent, "graph-Component.png");
        assertContent("component.expected", content);
    }

    @Test
    void renderSystemComponentPageWithEvents() throws IOException {
        SystemComponent systemComponent = BackendService.builder()
                .description("Description")
                .name("Component")
                .build();
        SystemComponent otherComponent1 = BackendService.builder()
                .name("OtherComponent1")
                .build();
        SystemComponent otherComponent2 = BackendService.builder()
                .name("OtherComponent2")
                .build();
        Event consumedEvent = Event.builder()
                .scope("public")
                .messageVersions(List.of())
                .messageTypeName("ConsumedEventName")
                .publisherContracts(List.of())
                .consumerContracts(List.of())
                .descriptorUrl("link")
                .build();
        Event providedEvent = Event.builder()
                .scope("public")
                .messageVersions(List.of())
                .messageTypeName("ProvidedEventName")
                .descriptorUrl("link")
                .publisherContracts(List.of())
                .consumerContracts(List.of())
                .build();

        EventRelation consumedEvent1 = EventRelation.builder()
                .consumerName(systemComponent.getName())
                .providerName(otherComponent1.getName())
                .eventName(consumedEvent.getMessageTypeName())
                .build();
        EventRelation providedEvent1 = EventRelation.builder()
                .providerName(systemComponent.getName())
                .consumerName(otherComponent1.getName())
                .eventName(providedEvent.getMessageTypeName())
                .build();
        EventRelation providedEvent2 = EventRelation.builder()
                .providerName(systemComponent.getName())
                .consumerName(otherComponent2.getName())
                .eventName(providedEvent.getMessageTypeName())
                .build();

        System system = System.builder()
                .name("System")
                .build();
        system.addSystemComponent(systemComponent);
        system.addSystemComponent(otherComponent1);
        system.addSystemComponent(otherComponent2);
        system.addEvent(consumedEvent);
        system.addEvent(providedEvent);
        system.addRelation(consumedEvent1);
        system.addRelation(providedEvent1);
        system.addRelation(providedEvent2);

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderComponentPage(model, systemComponent, "graph-Component.png");
        assertContent("componentwithevent.expected", content);
    }


    @Test
    void renderSystemComponentPageWithCommandsAndEvents() throws IOException {
        SystemComponent systemComponent = BackendService.builder()
                .description("Description")
                .name("Component")
                .build();
        SystemComponent otherComponent1 = BackendService.builder()
                .name("OtherComponent1")
                .build();
        SystemComponent otherComponent2 = BackendService.builder()
                .name("OtherComponent2")
                .build();
        SystemComponent otherComponent3 = BackendService.builder()
                .name("OtherComponent3")
                .build();
        SystemComponent otherComponent4 = BackendService.builder()
                .name("OtherComponent4")
                .build();
        System system = System.builder()
                .name("System")
                .build();

        system.addSystemComponent(systemComponent);

        system.addRelation(createEventRelation(systemComponent, otherComponent3, "XyzEvent"));

        system.addRelation(createEventRelation(systemComponent, otherComponent1, "ProvidedEventName"));
        system.addRelation(createEventRelation(systemComponent, otherComponent2, "ProvidedEventName"));
        system.addRelation(createEventRelation(systemComponent, otherComponent3, "ProvidedEventName"));
        system.addRelation(createEventRelation(systemComponent, otherComponent4, "ProvidedEventName"));

        system.addRelation(createEventRelation(systemComponent, otherComponent1, "FooBarEvent"));
        system.addRelation(createEventRelation(systemComponent, otherComponent2, "FooBarEvent"));
        system.addRelation(createEventRelation(systemComponent, otherComponent3, "FooBarEvent"));

        system.addRelation(createCommandRelation(systemComponent, otherComponent1, "FooBarCommand"));
        system.addRelation(createCommandRelation(systemComponent, otherComponent2, "FooBarCommand"));
        system.addRelation(createCommandRelation(systemComponent, otherComponent3, "FooBarCommand"));

        system.addRelation(createCommandRelation(systemComponent, otherComponent1, "OtherBarCommand"));
        system.addRelation(createCommandRelation(systemComponent, otherComponent2, "OtherBarCommand"));
        system.addRelation(createCommandRelation(systemComponent, otherComponent3, "OtherBarCommand"));
        system.addRelation(createCommandRelation(systemComponent, otherComponent4, "OtherBarCommand"));

        system.addRelation(createCommandRelation(systemComponent, otherComponent3, "XyzCommand"));

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderComponentPage(model, systemComponent, "graph-Component.png");
        assertContent("componentwithcommandevent.expected", content);
    }

    private EventRelation createEventRelation(SystemComponent provider, SystemComponent consumer, String messageTypeName){

        Event event = Event.builder()
                .scope("public")
                .messageVersions(List.of())
                .messageTypeName(messageTypeName)
                .descriptorUrl("link")
                .publisherContracts(List.of())
                .consumerContracts(List.of())
                .build();

        return EventRelation.builder()
                .providerName(provider.getName())
                .consumerName(consumer.getName())
                .eventName(event.getMessageTypeName())
                .build();
    }

    private CommandRelation createCommandRelation(SystemComponent provider, SystemComponent consumer, String messageTypeName){

        Command command = Command.builder()
                .scope("public")
                .messageVersions(List.of())
                .messageTypeName(messageTypeName)
                .descriptorUrl("link")
                .build();

        return CommandRelation.builder()
                .providerName(provider.getName())
                .consumerName(consumer.getName())
                .commandName(command.getMessageTypeName())
                .build();
    }


    @Test
    void renderSystemComponentPageWithCommands() throws IOException {
        SystemComponent systemComponent = BackendService.builder()
                .description("Description")
                .name("Component")
                .build();
        SystemComponent otherComponent1 = BackendService.builder()
                .name("OtherComponent1")
                .build();
        SystemComponent otherComponent2 = BackendService.builder()
                .name("OtherComponent2")
                .build();
        Command receivedCommand = Command.builder()
                .scope("public")
                .messageVersions(List.of())
                .messageTypeName("ReceivedCommandName")
                .descriptorUrl("link")
                .senderContracts(List.of())
                .receiverContracts(List.of())
                .build();
        Command sentCommand = Command.builder()
                .scope("public")
                .messageVersions(List.of())
                .messageTypeName("SentCommandName")
                .senderContracts(List.of())
                .receiverContracts(List.of())
                .descriptorUrl("link")
                .build();

        CommandRelation receivedCommandRelation = CommandRelation.builder()
                .consumerName(systemComponent.getName())
                .providerName(otherComponent1.getName())
                .commandName(receivedCommand.getMessageTypeName())
                .build();
        CommandRelation sentCommandRelation = CommandRelation.builder()
                .providerName(systemComponent.getName())
                .consumerName(otherComponent1.getName())
                .commandName(sentCommand.getMessageTypeName())
                .build();

        System system = System.builder()
                .name("System")
                .build();
        system.addSystemComponent(systemComponent);
        system.addSystemComponent(otherComponent1);
        system.addSystemComponent(otherComponent2);
        system.addCommand(sentCommand);
        system.addCommand(receivedCommand);
        system.addRelation(sentCommandRelation);
        system.addRelation(receivedCommandRelation);

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderComponentPage(model, systemComponent, "graph-Component.png");
        assertContent("componentwithcommand.expected", content);
    }

    @Test
    void renderSystemComponentPageWithNullConsumerProvider() throws IOException {
        SystemComponent component = BackendService.builder()
                .name("Component")
                .build();

        Event event = Event.builder()
                .scope("public")
                .messageVersions(List.of())
                .description("EventDesciption")
                .messageTypeName("EventName")
                .descriptorUrl("link")
                .documentationUrl("linkdoc")
                .topic("ConsumedTopic")
                .publisherContracts(List.of())
                .consumerContracts(List.of())
                .build();

        Event event2 = event.toBuilder()
                .messageTypeName("OtherEventName")
                .build();

        EventRelation relationNullConsumer = EventRelation.builder()
                .consumerName(null)
                .providerName(component.getName())
                .eventName(event.getMessageTypeName())
                .build();

        EventRelation relationEmptyConsumer = EventRelation.builder()
                .consumerName("")
                .providerName(component.getName())
                .eventName(event2.getMessageTypeName())
                .build();

        EventRelation relationNullProvider = EventRelation.builder()
                .consumerName(component.getName())
                .providerName(null)
                .eventName(event2.getMessageTypeName())
                .build();

        System system = System.builder()
                .name("System")
                .description("Description")
                .build();
        system.addEvent(event);
        system.addSystemComponent(component);
        system.addRelation(relationNullConsumer);
        system.addRelation(relationEmptyConsumer);
        system.addRelation(relationNullProvider);

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderComponentPage(model, component, "graph-Component.png");
        assertContent("componentwithnullconsumerprovider.expected", content);
    }

    @Test
    void renderSystemComponentPageWithConsumedAndProvidedRestApiRelations() throws IOException {
        BackendService systemComponent = BackendService.builder()
                .name("systemComponent")
                .build();
        BackendService consumer1 = BackendService.builder()
                .name("consumer1")
                .build();
        BackendService consumer2 = BackendService.builder()
                .name("consumer2")
                .build();
        BackendService consumer3 = BackendService.builder()
                .name("consumer3")
                .build();
        BackendService consumer4 = BackendService.builder()
                .name("consumer4")
                .build();
        System system = System.builder()
                .name("System")
                .description("Description")
                .build();

        system.addSystemComponent(systemComponent);
        system.addSystemComponent(consumer1);
        system.addSystemComponent(consumer2);
        system.addSystemComponent(consumer3);
        system.addSystemComponent(consumer4);

        //Incoming rest apis
        system.addRelation(createRestApiRelation(systemComponent, consumer1, "GET", "/foo/bar", "http://pact_outgoing1"));
        system.addRelation(createRestApiRelation(systemComponent, consumer2, "GET", "/foo/bar", "http://pact_outgoing2"));

        system.addRelation(createRestApiRelation(systemComponent, consumer1, "GET", "/api/incoming", "http://pact_outgoing1"));
        system.addRelation(createRestApiRelation(systemComponent, consumer2, "GET", "/api/incoming", "http://pact_outgoing2"));
        system.addRelation(createRestApiRelation(systemComponent, consumer3, "GET", "/api/incoming", "http://pact_outgoing3"));
        system.addRelation(createRestApiRelation(systemComponent, consumer4, "GET", "/api/incoming", null));

        system.addRelation(createRestApiRelation(systemComponent, consumer1, "PUT", "/api/incoming", "http://pact_outgoing1"));

        system.addRelation(createRestApiRelation(systemComponent, consumer1, "DELETE", "/api/incoming", null));
        system.addRelation(createRestApiRelation(systemComponent, consumer4, "DELETE", "/api/incoming", "http://pact_outgoing1"));

        system.addRelation(createRestApiRelation(systemComponent, consumer1, "GET", "/api/incoming/other", "http://pact_outgoing1"));
        system.addRelation(createRestApiRelation(systemComponent, consumer2, "GET", "/api/incoming/other", "http://pact_outgoing2"));
        system.addRelation(createRestApiRelation(systemComponent, consumer3, "GET", "/api/incoming/other", "http://pact_outgoing3"));

        system.addRelation(createRestApiRelation(systemComponent, consumer3, "GET", "/api/a/b", null));

        system.addRelation(createRestApiRelation(systemComponent, consumer1, "PUT", "/api/b/c", null));
        system.addRelation(createRestApiRelation(systemComponent, consumer2, "GET", "/api/b/c", null));
        system.addRelation(createRestApiRelation(systemComponent, consumer3, "DELETE", "/api/b/c", null));


        //Outgoing rest apis
        system.addRelation(createRestApiRelation(consumer1, systemComponent, "PUT", "/api/outgoing/value", "http://pact_outgoing"));
        system.addRelation(createRestApiRelation(consumer1, systemComponent, "GET", "/api/outgoing", null));

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderComponentPage(model, systemComponent, "graph-Component.png");
        assertContent("componentwithrestrelations.expected", content);
    }

    private RestApiRelation createRestApiRelation(SystemComponent provider, SystemComponent consumer, String method, String path, String pactUrl){
        RestApi restapi = RestApi.builder()
                .provider(provider)
                .method(method)
                .path(path)
                .importer(Importer.GRAFANA)
                .build();

        return RestApiRelation.builder()
                .consumerName(consumer.getName())
                .providerName(provider.getName())
                .restApi(restapi)
                .pactUrl(pactUrl)
                .lastSeen(ZonedDateTime.now())
                .build();
    }




    @Test
    void renderSystemComponentPageWithOpenApiUrl() throws IOException {
        BackendService systemComponent = BackendService.builder()
                .name("systemComponent")
                .build();
        System system = System.builder()
                .name("System")
                .description("Description")
                .build();
        system.addSystemComponent(systemComponent);

        OpenApiSpec openApiSpec = OpenApiSpec.builder()
                .provider(systemComponent)
                .content("content".getBytes(StandardCharsets.UTF_8))
                .build();

        system.addOpenApiSpec(openApiSpec);

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderComponentPage(model, systemComponent, "graph-Component.png");
        assertContent("componentwithOpenApiUrl.expected", content);
    }

    @Test
    void renderSystemComponentPageWithoutDatabaseSchema_assertNoSchema() {
        BackendService systemComponent = BackendService.builder()
                .name("systemComponent")
                .build();
        System system = System.builder()
                .name("System")
                .description("Description")
                .build();
        system.addSystemComponent(systemComponent);

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderComponentPage(model, systemComponent, "graph-Component.png");
        assertThat(content).contains("Kein Datenbankschema bekannt.");
    }

    @Test
    void renderSystemComponentPageWithDatabaseSchemaTooBig_assertNoSchema() throws IOException {
        DocumentationGeneratorConfiguration generatorConfig = new DocumentationGeneratorConfiguration();
        PlantUmlRenderer mockPlantUmlRenderer = mock(PlantUmlRenderer.class);
        RenderedDatabaseSchema mockSchema = mock(RenderedDatabaseSchema.class);
        when(mockSchema.length()).thenReturn(50001);
        when(mockPlantUmlRenderer.renderDatabaseSchema(any())).thenReturn(mockSchema);
        TemplateRenderer templateRendererWithMock = new TemplateRenderer(generatorConfig.templateEngine(applicationContext), mockPlantUmlRenderer);
        BackendService systemComponent = BackendService.builder()
                .name("systemComponent")
                .build();
        System system = System.builder()
                .name("System")
                .description("Description")
                .build();
        system.addSystemComponent(systemComponent);

        SystemComponentDatabaseSchema systemComponentDatabaseSchema = SystemComponentDatabaseSchema.builder()
                .systemComponent(systemComponent)
                .schema("mock".getBytes())
                .schemaVersion("1.2.3")
                .build();
        system.addDatabaseSchema(systemComponentDatabaseSchema);

        ArchitectureModel model = buildModel(system);

        String content = templateRendererWithMock.renderComponentPage(model, systemComponent, "graph-system.png");
        assertThat(content).contains("Datenbankschema zu gross (50001). Dokumentation wird nicht generiert.");
    }

    @Test
    void renderSystemComponentPageWithDatabaseSchema() throws IOException {
        BackendService systemComponent = BackendService.builder()
                .name("systemComponent")
                .build();
        System system = System.builder()
                .name("System")
                .description("Description")
                .build();
        system.addSystemComponent(systemComponent);

        DatabaseSchema schema = DatabaseSchema.builder()
                .name("foobar")
                .version("1.2.3")
                .tables(List.of(
                        Table.builder()
                                .name("foo")
                                .columns(List.of(
                                        TableColumn.builder()
                                                .name("bar")
                                                .type("int")
                                                .nullable(false)
                                                .build()))
                                .build()))
                .build();
        SystemComponentDatabaseSchema systemComponentDatabaseSchema = SystemComponentDatabaseSchema.builder()
                .systemComponent(systemComponent)
                .schema(schema.toJson())
                .schemaVersion("1.2.3")
                .build();
        system.addDatabaseSchema(systemComponentDatabaseSchema);

        ArchitectureModel model = buildModel(system);

        String content = templateRenderer.renderComponentPage(model, systemComponent, "graph-Component.png");
        assertContent("componentwithDatabaseSchema.expected", content);
    }

    @Test
    void renderEvent() throws IOException {
        Event event = Event.builder()
                .messageTypeName("TestEvent")
                .description("desc")
                .documentationUrl("url")
                .topic("topic")
                .descriptorUrl("link")
                .publisherContracts(List.of(MessageContract.builder().componentName("publisherService").topic("topic").version(List.of("1.2.0")).build()))
                .consumerContracts(List.of(MessageContract.builder().componentName("subscriberService").topic("topic").version(List.of("1.1.0", "1.2.0")).build()))
                .messageVersions(List.of(
                        MessageVersion.builder()
                                .version("1.1.0")
                                .valueSchemaName("value")
                                .valueSchemaUrl("linkS")
                                .valueSchemaResolved("Value Schema 1.1.0 Resolved with \"special\" <character> ...")
                                .build(),
                        MessageVersion.builder()
                                .version("1.2.0")
                                .keySchemaName("key")
                                .keySchemaUrl("linkS")
                                .valueSchemaName("value2")
                                .valueSchemaUrl("linkS")
                                .keySchemaResolved("Key Schema 1.2.0 Resolved with \"special\" <character> ...")
                                .valueSchemaResolved("Value Schema 1.2.0 Resolved with \"special\" <character> ...")
                                .compatibleVersion("1.1.0")
                                .compatibilityMode("BACKWARD")
                                .build()))
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .scope("public")
                .build();

        List<String> uploadedAttachmentNames = List.of("graph-event1-variant1.png", "graph-event1-variant2.png");

        String content = templateRenderer.renderEventPage(event, uploadedAttachmentNames);
        assertContent("event.expected", content);
    }

    @Test
    void renderCommand() throws IOException {
        Command command = Command.builder()
                .messageTypeName("TestCommand")
                .description("desc")
                .documentationUrl("url")
                .topic("topic")
                .descriptorUrl("link")
                .senderContracts(List.of(MessageContract.builder().componentName("senderService").topic("topic").version(List.of("1.2.0")).build()))
                .receiverContracts(List.of(MessageContract.builder().componentName("receiverService").topic("topic").version(List.of("1.1.0", "1.2.0")).build()))
                .messageVersions(List.of(
                        MessageVersion.builder()
                                .version("1.1.0")
                                .valueSchemaName("value")
                                .valueSchemaUrl("linkS")
                                .valueSchemaResolved("Value Schema 1.1.0 Resolved with \"special\" <character> ...")
                                .build(),
                        MessageVersion.builder()
                                .version("1.2.0")
                                .keySchemaName("key")
                                .keySchemaUrl("linkS")
                                .valueSchemaName("value2")
                                .valueSchemaUrl("linkS")
                                .keySchemaResolved("Key Schema 1.2.0 Resolved with \"special\" <character> ...")
                                .valueSchemaResolved("Value Schema 1.2.0 Resolved with \"special\" <character> ...")
                                .compatibleVersion("1.1.0")
                                .compatibilityMode("BACKWARD")
                                .build()))
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .scope("public")
                .build();

        List<String> uploadedAttachmentNames = List.of("graph-Command1-variant1.png", "graph-Command1-variant2.png");

        String content = templateRenderer.renderCommandPage(command, uploadedAttachmentNames);
        assertContent("command.expected", content);
    }

    @BeforeEach
    void setUp() {
        DocumentationGeneratorConfiguration generatorConfig = new DocumentationGeneratorConfiguration();
        templateRenderer = new TemplateRenderer(generatorConfig.templateEngine(applicationContext), new PlantUmlRenderer());
    }
}
