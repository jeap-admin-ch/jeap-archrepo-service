package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import java.util.List;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocsApiDtoFactoryTest {

    private DocsApiDtoFactory factory;
    private ArchitectureModel model;

    @BeforeEach
    void setUp() {
        factory = new DocsApiDtoFactory();
        model = DocsApiModelStub.create();
    }

    @Test
    void createSystemList_carriesTeamAndAliases() {
        SystemListDto systemList = factory.createSystemList(model);

        assertThat(systemList.systems()).hasSize(2);
        SystemSummaryDto system = systemList.systems().getFirst();
        assertThat(system.name()).isEqualTo(DocsApiModelStub.SYSTEM);
        assertThat(system.description()).isEqualTo("Warenverkehrssystem");
        assertThat(system.aliases()).containsExactly(DocsApiModelStub.SYSTEM_ALIAS);
        assertThat(system.team().name()).isEqualTo("Team Blue");
        assertThat(system.team().contactAddress()).isEqualTo("team-blue@example.com");
    }

    @Test
    void createSystemList_teamIsNullWhenTheSystemHasNoOwner() {
        SystemSummaryDto other = factory.createSystemList(model).systems().stream()
                .filter(system -> system.name().equals(DocsApiModelStub.OTHER_SYSTEM))
                .findFirst().orElseThrow();

        assertThat(other.team()).isNull();
    }

    @Test
    void createSystemDetail_componentInheritsTheSystemsDefaultOwner() {
        ComponentDto component = component(DocsApiModelStub.COMPONENT);

        // The component has no team of its own, so the generated page shows the system's default owner
        assertThat(component.team().name()).isEqualTo("Team Blue");
    }

    @Test
    void createSystemDetail_componentCarriesProvenance() {
        ComponentDto component = component(DocsApiModelStub.COMPONENT);

        assertThat(component.type()).isEqualTo(SystemComponentType.BACKEND_SERVICE);
        assertThat(component.importer()).isEqualTo(Importer.DEPLOYMENT_LOG);
        assertThat(component.lastSeen()).isNotNull();
    }

    @Test
    void createSystemDetail_restApisOfTheComponent() {
        assertThat(component(DocsApiModelStub.COMPONENT).restApis())
                .singleElement()
                .satisfies(restApi -> {
                    assertThat(restApi.method()).isEqualTo("GET");
                    assertThat(restApi.path()).isEqualTo("/api/foo/{id}");
                });
    }

    @Test
    void createSystemDetail_openApiRefWithARelativeContentUrlAndAnAbsoluteSwaggerUrl() {
        OpenApiRefDto openApi = component(DocsApiModelStub.COMPONENT).openApi();

        assertThat(openApi.version()).isEqualTo("1.4.2");
        assertThat(openApi.serverUrl()).isEqualTo("https://foo-bar.example.com");
        // Relative: a consumer resolves it against the base URL it already called
        assertThat(openApi.contentUrl())
                .isEqualTo("/docs-api/systems/wvs/components/wvs-foo-bar-service/openapi");
        // Absolute: a browser follows it, so it needs the host the instance configured
        assertThat(openApi.swaggerUrl())
                .isEqualTo(DocsApiModelStub.OPEN_API_BASE_URL.toLowerCase(java.util.Locale.ROOT)
                           + "wvs/wvs-foo-bar-service");
    }

    @Test
    void createSystemDetail_artifactRefsAreAbsentWithoutArtifacts() {
        ComponentDto plain = component(DocsApiModelStub.COMPONENT_WITHOUT_ARTIFACTS);

        assertThat(plain.openApi()).isNull();
        assertThat(plain.databaseSchema()).isNull();
        assertThat(plain.restApis()).isEmpty();
    }

    @Test
    void createSystemDetail_onlyActiveRelations() {
        SystemDetailDto detail = systemDetail();

        // The stub holds one active REST relation, one deleted REST relation and one event relation
        assertThat(detail.relations()).hasSize(2);
        assertThat(detail.relations()).extracting(RelationDto::type)
                .containsExactlyInAnyOrder(RelationType.REST_API_RELATION, RelationType.EVENT_RELATION);
    }

    @Test
    void createSystemDetail_restRelationCarriesMethodPathAndPact() {
        RelationDto rest = systemDetail().relations().stream()
                .filter(relation -> relation.type() == RelationType.REST_API_RELATION)
                .findFirst().orElseThrow();

        assertThat(rest.method()).isEqualTo("GET");
        assertThat(rest.path()).isEqualTo("/api/foo/{id}");
        assertThat(rest.pactUrl()).isEqualTo("https://pactbroker.example.com/pacts/foo");
        assertThat(rest.consumer()).isEqualTo(DocsApiModelStub.OTHER_COMPONENT);
        assertThat(rest.consumerSystem()).isEqualTo(DocsApiModelStub.OTHER_SYSTEM);
        assertThat(rest.messageType()).isNull();
    }

    @Test
    void createSystemDetail_eventRelationCarriesTheMessageTypeAndNoRestFields() {
        RelationDto event = systemDetail().relations().stream()
                .filter(relation -> relation.type() == RelationType.EVENT_RELATION)
                .findFirst().orElseThrow();

        assertThat(event.messageType()).isEqualTo(DocsApiModelStub.EVENT_NAME);
        assertThat(event.method()).isNull();
        assertThat(event.path()).isNull();
        assertThat(event.pactUrl()).isNull();
    }

    @Test
    void createMessageList_eventContractsUsePublisherAndConsumer() {
        MessageDto event = message(DocsApiModelStub.EVENT_NAME);

        assertThat(event.kind()).isEqualTo("EVENT");
        assertThat(event.versions()).containsExactly("1.0.0", "2.0.0");
        assertThat(event.contracts()).extracting(MessageContractDto::role)
                .containsExactlyInAnyOrder("PUBLISHER", "CONSUMER");
    }

    @Test
    void createMessageList_commandContractsUseSenderAndReceiver() {
        MessageDto command = message(DocsApiModelStub.COMMAND_NAME);

        assertThat(command.kind()).isEqualTo("COMMAND");
        assertThat(command.contracts()).extracting(MessageContractDto::role).containsExactly("SENDER");
    }

    @Test
    void createMessageList_contractResolvesTheOwningSystemOfAComponent() {
        MessageContractDto consumer = message(DocsApiModelStub.EVENT_NAME).contracts().stream()
                .filter(contract -> contract.role().equals("CONSUMER"))
                .findFirst().orElseThrow();

        assertThat(consumer.component()).isEqualTo(DocsApiModelStub.OTHER_COMPONENT);
        assertThat(consumer.system()).isEqualTo(DocsApiModelStub.OTHER_SYSTEM);
        assertThat(consumer.versions()).containsExactly("1.0.0", "2.0.0");
    }

    private SystemDetailDto systemDetail() {
        return factory.createSystemDetail(model, wvs());
    }

    private ComponentDto component(String name) {
        return systemDetail().components().stream()
                .filter(component -> component.name().equals(name))
                .findFirst().orElseThrow();
    }

    private MessageDto message(String name) {
        return factory.createMessageList(model, wvs()).messages().stream()
                .filter(message -> message.name().equals(name))
                .findFirst().orElseThrow();
    }

    private System wvs() {
        return model.getSystems().stream()
                .filter(system -> system.getName().equals(DocsApiModelStub.SYSTEM))
                .findFirst().orElseThrow();
    }

    @Test
    void messageVersionsAreSorted() {
        // The versions come from a JPA element collection with no @OrderBy, so the payload has to impose an order
        assertThat(message(DocsApiModelStub.EVENT_NAME).versions()).containsExactly("1.0.0", "2.0.0");
    }

    @Test
    void thePayloadOrderIsIndependentOfTheOrderTheModelComesBackIn() {
        // The entity tag of the model resources is the hash of the serialized body, so the body has to be
        // byte-identical for an unchanged landscape - and neither findAll() nor the JPA bags promise an order.
        SystemListDto fromOneOrder = factory.createSystemList(model);
        SystemListDto fromShuffledOrder = factory.createSystemList(reversed(model));

        assertThat(fromOneOrder).isEqualTo(fromShuffledOrder);
        assertThat(factory.createSystemDetail(model, wvs()))
                .isEqualTo(factory.createSystemDetail(reversed(model), wvs()));
        assertThat(factory.createMessageList(model, wvs()))
                .isEqualTo(factory.createMessageList(reversed(model), wvs()));

        // ... and the nested collections too: reversing only the top-level list would not guard them
        ArchitectureModel deeplyShuffled = DocsApiModelStub.createWithNestedCollectionsReversed();
        assertThat(factory.createMessageList(deeplyShuffled, systemOf(deeplyShuffled)))
                .isEqualTo(factory.createMessageList(model, wvs()));
        assertThat(factory.createSystemDetail(deeplyShuffled, systemOf(deeplyShuffled)))
                .isEqualTo(factory.createSystemDetail(model, wvs()));
    }

    private ch.admin.bit.jeap.archrepo.metamodel.System systemOf(ArchitectureModel source) {
        return source.getSystems().stream()
                .filter(system -> system.getName().equals(DocsApiModelStub.SYSTEM))
                .findFirst().orElseThrow();
    }

    private ArchitectureModel reversed(ArchitectureModel source) {
        List<ch.admin.bit.jeap.archrepo.metamodel.System> systems =
                new java.util.ArrayList<>(source.getSystems());
        java.util.Collections.reverse(systems);
        return ArchitectureModel.builder()
                .systems(systems)
                .teams(java.util.List.of())
                .openApiBaseUrl(DocsApiModelStub.OPEN_API_BASE_URL)
                .build();
    }
}
