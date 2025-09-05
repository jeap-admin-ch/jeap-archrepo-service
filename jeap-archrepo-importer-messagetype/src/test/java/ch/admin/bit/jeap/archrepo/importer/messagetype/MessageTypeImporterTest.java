package ch.admin.bit.jeap.archrepo.importer.messagetype;

import ch.admin.bit.jeap.archrepo.importer.messagetype.contractservice.ContractServiceClient;
import ch.admin.bit.jeap.archrepo.importer.messagetype.repository.MessageTypeRepositoryFactory;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

class MessageTypeImporterTest extends CreateLocalGitRepoBaseTest {

    private static MessageTypeImporter messageTypeImporter;
    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setUp() {

        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        int messageContractServicePort = wireMockServer.port();

        MessageTypeImporterProperties properties = new MessageTypeImporterProperties(
                List.of(repoUrl),
                "http://localhost:" + messageContractServicePort + "/contracts",
                List.of(new RepositoryProperties(repoUrl, RepositoryProperties.RepositoryType.BITBUCKET)));
        MessageTypeRepositoryFactory repositoryFactory = new MessageTypeRepositoryFactory(properties);
        ContractServiceClient contractServiceClient = new ContractServiceClient(properties, RestClient.builder());
        MessageContractImporter messageContractImporter = new MessageContractImporter(contractServiceClient);
        messageTypeImporter = new MessageTypeImporter(repositoryFactory, messageContractImporter);
    }

    @Test
    void shouldImportMessageTypesAndContractsAndCreateRelations() {
        // given
        wireMockServer.stubFor(get(urlPathEqualTo("/contracts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                    {"appName": "svc-1", "messageType": "TestCommand", "messageTypeVersion": "2.0.0", "topic": "test-command-topic", "role": "PRODUCER"},
                                    {"appName": "svc-4", "messageType": "TestCommand", "messageTypeVersion": "2.0.0", "topic": "svc4-command-topic", "role": "CONSUMER"},
                                    {"appName": "svc-2", "messageType": "TestCommand", "messageTypeVersion": "2.0.0", "topic": "test-command-topic", "role": "CONSUMER"},
                                    {"appName": "svc-2", "messageType": "TestEvent", "messageTypeVersion": "2.0.0", "topic": "test-event-topic", "role": "PRODUCER"},
                                    {"appName": "svc-4", "messageType": "TestEvent", "messageTypeVersion": "2.0.0", "topic": "svc4-command-topic", "role": "PRODUCER"},
                                    {"appName": "svc-3", "messageType": "TestEvent", "messageTypeVersion": "2.0.0", "topic": "test-event-topic", "role": "CONSUMER"}
                                ]
                                """)));

        ArchitectureModel model = createTestModel();

        // when
        messageTypeImporter.importIntoModel(model, "ref");

        // then
        assertThat(model.getAllMessageTypes()).hasSize(2);
        assertThat(model.getAllMessageTypes().stream().map(MessageType::getMessageTypeName).toList())
                .containsExactlyInAnyOrder("TestCommand", "TestEvent");

        System input = model.getSystems().getFirst();
        assertThat(input.getEvents()).hasSize(1);
        assertThat(input.getEvents().getFirst().getConsumerContracts()).hasSize(1);
        assertThat(input.getEvents().getFirst().getPublisherContracts()).hasSize(2);
        assertThat(input.getEvents().getFirst().getMessageVersions().get(1))
                .matches(v -> v.getCompatibilityMode().equals("BACKWARD"))
                .matches(v -> v.getCompatibleVersion().equals("1.0.0"));
        assertThat(input.getCommands()).hasSize(1);
        assertThat(input.getCommands().getFirst().getReceiverContracts()).hasSize(2);
        assertThat(input.getCommands().getFirst().getSenderContracts()).hasSize(1);
        assertThat(input.getCommands().getFirst().getMessageVersions().get(1))
                .matches(v -> v.getCompatibilityMode().equals("BACKWARD"))
                .matches(v -> v.getCompatibleVersion().equals("1.0.0"));
        assertThat(input.getCommands().getFirst().getMessageVersions().get(2))
                .matches(v -> v.getCompatibilityMode().equals("BACKWARD"))
                .matches(v -> v.getCompatibleVersion().equals("2.0.0"));

        assertThat(model.getAllRelations()).hasSize(4);
        assertRelation(model, "svc-1", "svc-2", RelationType.COMMAND_RELATION);
        assertRelation(model, "svc-2", "svc-3", RelationType.EVENT_RELATION);
        assertRelation(model, null, "svc-4", RelationType.COMMAND_RELATION);
        assertRelation(model, "svc-4", null, RelationType.EVENT_RELATION);
    }

    private void assertRelation(ArchitectureModel model, String providerName, String consumerName, RelationType type) {
        assertTrue(model.getAllRelations().stream().anyMatch(r ->
                Objects.equals(r.getProviderName(), providerName) && Objects.equals(r.getConsumerName(), consumerName) && r.getType().equals(type)
        ));
    }

    private ArchitectureModel createTestModel() {
        SystemComponent component1 = BackendService.builder().name("svc-1").build();
        SystemComponent component2 = BackendService.builder().name("svc-2").build();
        SystemComponent component3 = BackendService.builder().name("svc-3").build();
        SystemComponent inputDocScs = BackendService.builder().name("svc-4").build();
        System system = System.builder()
                .name("TESTSYSTEM")
                .systemComponents(List.of(component1, component2, component3, inputDocScs))
                .build();
        return ArchitectureModel.builder()
                .systems(List.of(system))
                .build();
    }
}
