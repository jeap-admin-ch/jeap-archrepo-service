package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.persistence.MessageGraphRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
        ReactionsObserverImporterConfiguration.class,
        TestConfig.class
}, properties = {
        "reactionobserverservice.url=http://localhost:${wiremock.server.port}/reaction-observer-service",
        "reactionobserverservice.username=user",
        "reactionobserverservice.password=secret",
        "spring.application.name=test"})
@AutoConfigureWireMock(port = 0)
class MessageGraphImporterWireMockTest {

    @Autowired
    private MessageGraphImporter importer;

    @Autowired
    private MessageGraphRepository messageGraphRepository;

    @BeforeEach
    void setUp() {
        reset(messageGraphRepository);
        when(messageGraphRepository.save(any(MessageGraph.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageGraphRepository.existsByMessageTypeNameAndVariant(any(), any())).thenReturn(false);
    }

    @Test
    void importIntoModel_withMessageGraphs_savesMessageGraphs() {
        // Arrange
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());

        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/messages/UserCreatedEvent"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "UserCreatedEvent":
                                    {
                                      "graph": {
                                        "nodes": [
                                             {
                                               "nodeType": "MESSAGE",
                                               "id": 123,
                                               "messageKey": "msg_123",
                                               "messageType": "UserCreatedEvent",
                                               "variant": "default"
                                             },
                                             {
                                               "nodeType": "REACTION",
                                               "id": 77,
                                               "component": "notification-service"
                                             }
                                           ],
                                        "edges": [
                                           {
                                             "edgeType": "TRIGGER",
                                             "sourceId": 123,
                                             "sourceNodeType": "MESSAGE",
                                             "targetReactionId": 77,
                                             "median": 10
                                           },
                                           {
                                             "edgeType": "ACTION",
                                             "sourceReactionId": 77,
                                             "targetId": 123,
                                             "targetNodeType": "REACTION"
                                           }
                                         ]
                                      },
                                      "fingerprint": "fingerprint-message1"
                                    }
                                }
                                """)));

        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/messages/OrderProcessedEvent"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "OrderProcessedEvent":
                                    {
                                      "graph": {
                                        "nodes": [
                                             {
                                               "nodeType": "MESSAGE",
                                               "id": 1234,
                                               "messageKey": "msg_1234",
                                               "messageType": "OrderProcessedEvent",
                                               "variant": "default"
                                             },
                                             {
                                               "nodeType": "REACTION",
                                               "id": 778,
                                               "component": "inventory-service"
                                             }
                                           ],
                                        "edges": [
                                           {
                                             "edgeType": "TRIGGER",
                                             "sourceId": 1234,
                                             "sourceNodeType": "MESSAGE",
                                             "targetReactionId": 778,
                                             "median": 15
                                           },
                                           {
                                             "edgeType": "ACTION",
                                             "sourceReactionId": 778,
                                             "targetId": 1234,
                                             "targetNodeType": "REACTION"
                                           }
                                         ]
                                      },
                                      "fingerprint": "fingerprint-message2"
                                      }
                                    }
                                """)));

        // Create test model with mock message types
        ArchitectureModel testModel = createTestModelWithMessageTypes(
                List.of("UserCreatedEvent", "OrderProcessedEvent")
        );

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        verify(messageGraphRepository, times(2)).existsByMessageTypeNameAndVariant(any(), any());

        ArgumentCaptor<MessageGraph> messageGraphCaptor = ArgumentCaptor.forClass(MessageGraph.class);
        verify(messageGraphRepository, times(2)).save(messageGraphCaptor.capture());

        List<MessageGraph> savedGraphs = messageGraphCaptor.getAllValues();

        // Verify UserCreatedEvent graph
        MessageGraph userEventGraph = savedGraphs.stream()
                .filter(g -> "UserCreatedEvent".equals(g.getMessageTypeName()))
                .findFirst()
                .orElseThrow();
        assertThat(userEventGraph.getMessageTypeName()).isEqualTo("UserCreatedEvent");
        assertThat(userEventGraph.getVariant()).isEqualTo("");
        assertThat(userEventGraph.getFingerprint()).isEqualTo("fingerprint-message1");
        assertThat(userEventGraph.getGraphData()).isNotNull();

        // Verify the serialized graph data contains expected content
        String userEventGraphJson = new String(userEventGraph.getGraphData());
        assertThat(userEventGraphJson)
                .contains("notification-service")
                .contains("UserCreatedEvent");

        // Verify OrderProcessedEvent graph
        MessageGraph orderEventGraph = savedGraphs.stream()
                .filter(g -> "OrderProcessedEvent".equals(g.getMessageTypeName()))
                .findFirst()
                .orElseThrow();
        assertThat(orderEventGraph.getMessageTypeName()).isEqualTo("OrderProcessedEvent");
        assertThat(orderEventGraph.getFingerprint()).isEqualTo("fingerprint-message2");
        assertThat(orderEventGraph.getVariant()).isEqualTo("");
        assertThat(orderEventGraph.getGraphData()).isNotNull();

        // Verify the serialized graph data contains expected content
        String orderEventGraphJson = new String(orderEventGraph.getGraphData());
        assertThat(orderEventGraphJson)
                .contains("inventory-service")
                .contains("OrderProcessedEvent");
    }

    @Test
    void importIntoModel_severalVariants_savesMessageGraphs() {
        // Arrange
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());

        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/messages/UserCreatedEvent"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "UserCreatedEvent":
                                    {
                                      "graph": {
                                        "nodes": [
                                             {
                                               "nodeType": "MESSAGE",
                                               "id": 123,
                                               "messageKey": "msg_123",
                                               "messageType": "UserCreatedEvent",
                                               "variant": "default"
                                             },
                                             {
                                               "nodeType": "REACTION",
                                               "id": 77,
                                               "component": "notification-service"
                                             }
                                           ],
                                        "edges": [
                                           {
                                             "edgeType": "TRIGGER",
                                             "sourceId": 123,
                                             "sourceNodeType": "MESSAGE",
                                             "targetReactionId": 77,
                                             "median": 10
                                           },
                                           {
                                             "edgeType": "ACTION",
                                             "sourceReactionId": 77,
                                             "targetId": 123,
                                             "targetNodeType": "REACTION"
                                           }
                                         ]
                                      },
                                      "fingerprint": "fingerprint-message1"
                                    },
                                  "variant1":
                                    {
                                      "graph": {
                                        "nodes": [
                                             {
                                               "nodeType": "MESSAGE",
                                               "id": 123,
                                               "messageKey": "msg_123",
                                               "messageType": "UserCreatedEvent",
                                               "variant": "variant1"
                                             },
                                             {
                                               "nodeType": "REACTION",
                                               "id": 77,
                                               "component": "notification-service"
                                             }
                                           ],
                                        "edges": [
                                           {
                                             "edgeType": "TRIGGER",
                                             "sourceId": 123,
                                             "sourceNodeType": "MESSAGE",
                                             "targetReactionId": 77,
                                             "median": 10
                                           },
                                           {
                                             "edgeType": "ACTION",
                                             "sourceReactionId": 77,
                                             "targetId": 123,
                                             "targetNodeType": "REACTION"
                                           }
                                         ]
                                      },
                                      "fingerprint": "fingerprint-message2"
                                    }                                    
                                }
                                """)));

        // Create test model with mock message types
        ArchitectureModel testModel = createTestModelWithMessageTypes(List.of("UserCreatedEvent"));

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        verify(messageGraphRepository, times(2)).existsByMessageTypeNameAndVariant(any(), any());

        ArgumentCaptor<MessageGraph> messageGraphCaptor = ArgumentCaptor.forClass(MessageGraph.class);
        verify(messageGraphRepository, times(2)).save(messageGraphCaptor.capture());

        List<MessageGraph> savedGraphs = messageGraphCaptor.getAllValues();

        // Verify UserCreatedEvent with no variant graph
        MessageGraph userEventGraph = savedGraphs.stream()
                .filter(g -> "UserCreatedEvent".equals(g.getMessageTypeName()))
                .findFirst()
                .orElseThrow();
        assertThat(userEventGraph.getMessageTypeName()).isEqualTo("UserCreatedEvent");
        assertThat(userEventGraph.getVariant()).isEqualTo("");
        assertThat(userEventGraph.getFingerprint()).isEqualTo("fingerprint-message1");
        assertThat(userEventGraph.getGraphData()).isNotNull();

        // Verify the serialized graph data contains expected content
        String userEventGraphJson = new String(userEventGraph.getGraphData());
        assertThat(userEventGraphJson)
                .contains("notification-service")
                .contains("UserCreatedEvent");

        // Verify OrderProcessedEvent graph
        userEventGraph = savedGraphs.get(1);
        assertThat(userEventGraph.getMessageTypeName()).isEqualTo("UserCreatedEvent");
        assertThat(userEventGraph.getVariant()).isEqualTo("variant1");
        assertThat(userEventGraph.getFingerprint()).isEqualTo("fingerprint-message2");
        assertThat(userEventGraph.getGraphData()).isNotNull();

        // Verify the serialized graph data contains expected content
        assertThat(userEventGraphJson)
                .contains("notification-service")
                .contains("UserCreatedEvent");

    }

    @Test
    void importIntoModel_withMessageGraphEndpointReturning404_skipsSave() {
        // Arrange
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());

        // Stub for message graph returning 404
        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/messages/UnknownEvent"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withBody("")));

        ArchitectureModel testModel = createTestModelWithMessageTypes(List.of("UnknownEvent"));

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        verify(messageGraphRepository, never()).existsByMessageTypeNameAndVariant(any(), any());
        verify(messageGraphRepository, never()).save(any());
        verify(messageGraphRepository, never()).updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(any(), any(), any(), any());
    }

    @Test
    void importIntoModel_withExistingMessageGraph_updatesMessageGraph() {
        // Arrange
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());

        // Stub for message graph
        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/messages/ExistingEvent"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "ExistingEvent":
                                    {
                                      "graph": {
                                        "nodes": ["updated-node"],
                                        "edges": []
                                      },
                                      "fingerprint": "updated-fingerprint"
                                    }
                                }
                                """)));

        // Mock existing message
        when(messageGraphRepository.existsByMessageTypeNameAndVariant("ExistingEvent", "")).thenReturn(true);

        ArchitectureModel testModel = createTestModelWithMessageTypes(List.of("ExistingEvent"));

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        verify(messageGraphRepository).existsByMessageTypeNameAndVariant("ExistingEvent", "");
        verify(messageGraphRepository).updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(eq("ExistingEvent"), eq(""), any(byte[].class), eq("updated-fingerprint"));
        verify(messageGraphRepository, never()).save(any());
    }

    @Test
    void importIntoModel_withNoMessageTypes_doesNotCallRepository() {
        // Arrange
        ArchitectureModel testModel = createTestModelWithMessageTypes(List.of());

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        verify(messageGraphRepository, never()).existsByMessageTypeNameAndVariant(any(), any());
        verify(messageGraphRepository, never()).save(any());
        verify(messageGraphRepository, never()).updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(any(), any(), any(), any());
    }

    private ArchitectureModel createTestModelWithMessageTypes(List<String> messageTypeNames) {
        // Create mock message types for the test
        List<MessageType> messageTypes = messageTypeNames.stream()
                .map(name -> {
                    MessageType mockMessageType = mock(MessageType.class);
                    when(mockMessageType.getMessageTypeName()).thenReturn(name);
                    return mockMessageType;
                })
                .toList();

        ArchitectureModel mockModel = mock(ArchitectureModel.class);
        when(mockModel.getAllMessageTypes()).thenReturn(messageTypes);
        return mockModel;
    }
}
