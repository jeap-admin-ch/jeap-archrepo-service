package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.importer.reaction.client.GraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.MessageGraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.persistence.MessageGraphRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageGraphImporterTest {

    @Mock
    private ReactionObserverService reactionObserverService;

    @Mock
    private MessageGraphRepository messageGraphRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ArchitectureModel model;

    @Mock
    private MessageType messageType1;

    @Mock
    private MessageType messageType2;

    @Captor
    private ArgumentCaptor<MessageGraph> messageGraphCaptor;

    @Test
    void importIntoModel_withNewMessageGraph_savesMessageGraph() throws JsonProcessingException {
        // Arrange
        String messageTypeName = "TestEvent";
        String variant = "";
        when(messageType1.getMessageTypeName()).thenReturn(messageTypeName);
        when(model.getAllMessageTypes()).thenReturn(List.of(messageType1));
        when(messageGraphRepository.existsByMessageTypeNameAndVariant(messageTypeName, variant)).thenReturn(false);

        Map<String, Object> messageNode = Map.of(
            "nodeType", "MESSAGE",
            "id", 123,
            "messageKey", "msg_123",
            "messageType", "TestEvent",
            "variant", "default"
        );
        Map<String, Object> reactionNode = Map.of(
            "nodeType", "REACTION",
            "id", 77,
            "component", "test-component"
        );
        Map<String, Object> triggerEdge = Map.of(
            "edgeType", "TRIGGER",
            "sourceId", 123,
            "sourceNodeType", "MESSAGE",
            "targetReactionId", 77,
            "median", 10
        );
        Map<String, Object> actionEdge = Map.of(
            "edgeType", "ACTION",
            "sourceReactionId", 77,
            "targetId", 123,
            "targetNodeType", "REACTION"
        );

        Map<String, Object> graphData = Map.of(
            "nodes", List.of(messageNode, reactionNode),
            "edges", List.of(triggerEdge, actionEdge)
        );
        GraphDto graphDto = new GraphDto(graphData, "fingerprint123");
        MessageGraphDto messageGraphDto = new MessageGraphDto();
        messageGraphDto.put(messageTypeName, graphDto);
        when(reactionObserverService.getMessageGraph(messageTypeName)).thenReturn(messageGraphDto);

        String expectedSerializedData = "{\"nodes\":[{\"nodeType\":\"MESSAGE\",\"id\":123,\"messageKey\":\"msg_123\",\"messageType\":\"TestEvent\",\"variant\":\"default\"},{\"nodeType\":\"REACTION\",\"id\":77,\"component\":\"test-component\"}],\"edges\":[{\"edgeType\":\"TRIGGER\",\"sourceId\":123,\"sourceNodeType\":\"MESSAGE\",\"targetReactionId\":77,\"median\":10},{\"edgeType\":\"ACTION\",\"sourceReactionId\":77,\"targetId\":123,\"targetNodeType\":\"REACTION\"}]}";
        byte[] serializedData = expectedSerializedData.getBytes();
        when(objectMapper.writeValueAsBytes(graphData)).thenReturn(serializedData);

        MessageGraphImporter importer = new MessageGraphImporter(reactionObserverService, messageGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(messageGraphRepository).existsByMessageTypeNameAndVariant(messageTypeName, variant);
        verify(messageGraphRepository).save(messageGraphCaptor.capture());
        verify(messageGraphRepository, never()).updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(any(), any(), any(), any());

        MessageGraph capturedMessageGraph = messageGraphCaptor.getValue();
        assertThat(capturedMessageGraph.getMessageTypeName()).isEqualTo(messageTypeName);
        assertThat(capturedMessageGraph.getGraphData()).isEqualTo(serializedData);
        assertThat(capturedMessageGraph.getFingerprint()).isEqualTo("fingerprint123");
    }

    @Test
    void importIntoModel_withExistingMessageGraph_updatesMessageGraph() throws JsonProcessingException {
        // Arrange
        String messageTypeName = "UpdatedEvent";
        String variant = "";
        when(messageType1.getMessageTypeName()).thenReturn(messageTypeName);
        when(model.getAllMessageTypes()).thenReturn(List.of(messageType1));
        when(messageGraphRepository.existsByMessageTypeNameAndVariant(messageTypeName, variant)).thenReturn(true);

        Map<String, Object> messageNode = Map.of(
            "nodeType", "MESSAGE",
            "id", 124,
            "messageKey", "msg_124",
            "messageType", "UpdatedEvent",
            "variant", "default"
        );
        Map<String, Object> reactionNode = Map.of(
            "nodeType", "REACTION",
            "id", 78,
            "component", "updated-component"
        );
        Map<String, Object> triggerEdge = Map.of(
            "edgeType", "TRIGGER",
            "sourceId", 124,
            "sourceNodeType", "MESSAGE",
            "targetReactionId", 78,
            "median", 15
        );
        Map<String, Object> actionEdge = Map.of(
            "edgeType", "ACTION",
            "sourceReactionId", 78,
            "targetId", 124,
            "targetNodeType", "REACTION"
        );

        Map<String, Object> graphData = Map.of(
            "nodes", List.of(messageNode, reactionNode),
            "edges", List.of(triggerEdge, actionEdge)
        );
        GraphDto graphDto = new GraphDto(graphData, "fingerprint123");
        MessageGraphDto messageGraphDto = new MessageGraphDto();
        messageGraphDto.put(messageTypeName, graphDto);
        when(reactionObserverService.getMessageGraph(messageTypeName)).thenReturn(messageGraphDto);

        String expectedSerializedData = "{\"nodes\":[{\"nodeType\":\"MESSAGE\",\"id\":124,\"messageKey\":\"msg_124\",\"messageType\":\"UpdatedEvent\",\"variant\":\"default\"},{\"nodeType\":\"REACTION\",\"id\":78,\"component\":\"updated-component\"}],\"edges\":[{\"edgeType\":\"TRIGGER\",\"sourceId\":124,\"sourceNodeType\":\"MESSAGE\",\"targetReactionId\":78,\"median\":15},{\"edgeType\":\"ACTION\",\"sourceReactionId\":78,\"targetId\":124,\"targetNodeType\":\"REACTION\"}]}";
        byte[] serializedData = expectedSerializedData.getBytes();
        when(objectMapper.writeValueAsBytes(graphData)).thenReturn(serializedData);

        MessageGraphImporter importer = new MessageGraphImporter(reactionObserverService, messageGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(messageGraphRepository).existsByMessageTypeNameAndVariant(messageTypeName, variant);
        verify(messageGraphRepository).updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(messageTypeName, variant, serializedData, "fingerprint123");
        verify(messageGraphRepository, never()).save(any());
    }

    @Test
    void importIntoModel_withoutMessageGraphs_doesNotSaveAnything() {
        // Arrange
        String messageTypeName = "TestEvent";
        when(messageType1.getMessageTypeName()).thenReturn(messageTypeName);
        when(model.getAllMessageTypes()).thenReturn(List.of(messageType1));
        when(reactionObserverService.getMessageGraph(messageTypeName)).thenReturn(null);

        MessageGraphImporter importer = new MessageGraphImporter(
                reactionObserverService, messageGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(messageGraphRepository, never()).save(any());
        verify(messageGraphRepository, never()).updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(any(), any(), any(), any());
    }

    @Test
    void importIntoModel_withMultipleMessageTypes_processesAllMessageTypes() throws JsonProcessingException {
        // Arrange
        String messageType1Name = "Event1";
        String messageType2Name = "Event2";
        String variant = "";
        when(messageType1.getMessageTypeName()).thenReturn(messageType1Name);
        when(messageType2.getMessageTypeName()).thenReturn(messageType2Name);
        when(model.getAllMessageTypes()).thenReturn(List.of(messageType1, messageType2));
        when(messageGraphRepository.existsByMessageTypeNameAndVariant(messageType1Name, variant)).thenReturn(false);
        when(messageGraphRepository.existsByMessageTypeNameAndVariant(messageType2Name, variant)).thenReturn(true);

        Map<String, Object> graphData1 = Map.of("nodes", List.of("node1"));
        Map<String, Object> graphData2 = Map.of("nodes", List.of("node2"));

        MessageGraphDto messageGraphDto1 = new MessageGraphDto();
        messageGraphDto1.put(messageType1Name, new GraphDto(graphData1, "fingerprint1"));
        MessageGraphDto messageGraphDto2 = new MessageGraphDto();
        messageGraphDto2.put(messageType2Name, new GraphDto(graphData2, "fingerprint2"));

        when(reactionObserverService.getMessageGraph(messageType1Name)).thenReturn(messageGraphDto1);
        when(reactionObserverService.getMessageGraph(messageType2Name)).thenReturn(messageGraphDto2);

        byte[] serializedData1 = "{\"nodes\":[\"node1\"]}".getBytes();
        byte[] serializedData2 = "{\"nodes\":[\"node2\"]}".getBytes();

        when(objectMapper.writeValueAsBytes(graphData1)).thenReturn(serializedData1);
        when(objectMapper.writeValueAsBytes(graphData2)).thenReturn(serializedData2);

        MessageGraphImporter importer = new MessageGraphImporter(
                reactionObserverService, messageGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(messageGraphRepository).existsByMessageTypeNameAndVariant(messageType1Name, variant);
        verify(messageGraphRepository).existsByMessageTypeNameAndVariant(messageType2Name, variant);
        verify(messageGraphRepository).save(any()); // messageType1 gets saved as new
        verify(messageGraphRepository).updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(messageType2Name, variant, serializedData2, "fingerprint2"); // messageType2 gets updated
    }

    @Test
    void importIntoModel_withSerializationError_continuesProcessing() throws JsonProcessingException {
        // Arrange
        String messageTypeName = "TestEvent";
        when(messageType1.getMessageTypeName()).thenReturn(messageTypeName);
        when(model.getAllMessageTypes()).thenReturn(List.of(messageType1));

        Map<String, Object> graphData = Map.of("nodes", List.of("node1"));
        MessageGraphDto messageGraphDto = new MessageGraphDto();
        messageGraphDto.put(messageTypeName, new GraphDto(graphData, "fingerprint123"));
        when(reactionObserverService.getMessageGraph(messageTypeName)).thenReturn(messageGraphDto);

        when(objectMapper.writeValueAsBytes(graphData)).thenThrow(new JsonProcessingException("Serialization error") {});

        MessageGraphImporter importer = new MessageGraphImporter(
                reactionObserverService, messageGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(messageGraphRepository, never()).save(any());
        verify(messageGraphRepository, never()).updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(any(), any(), any(), any());
    }

    @Test
    void importIntoModel_withNoMessageTypes_doesNotSaveOrUpdate() {
        // Arrange
        when(model.getAllMessageTypes()).thenReturn(Collections.emptyList());

        MessageGraphImporter importer = new MessageGraphImporter(
                reactionObserverService, messageGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(messageGraphRepository, never()).save(any());
        verify(messageGraphRepository, never()).updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(any(), any(), any(), any());
    }

    @Test
    void importIntoModel_withVariantContainingSlash_extractsVariantCorrectly() throws JsonProcessingException {
        // Arrange
        String messageTypeName = "Event1";
        String rawVariant = "Event1/v2";
        String expectedVariant = "v2";

        when(messageType1.getMessageTypeName()).thenReturn(messageTypeName);
        when(model.getAllMessageTypes()).thenReturn(List.of(messageType1));

        Map<String, Object> graphData = Map.of("nodes", List.of("node1"));
        MessageGraphDto messageGraphDto = new MessageGraphDto();
        messageGraphDto.put(rawVariant, new GraphDto(graphData, "fingerprint123"));

        when(reactionObserverService.getMessageGraph(messageTypeName)).thenReturn(messageGraphDto);
        when(objectMapper.writeValueAsBytes(graphData)).thenReturn("{\"nodes\":[\"node1\"]}".getBytes());
        when(messageGraphRepository.existsByMessageTypeNameAndVariant(messageTypeName, expectedVariant)).thenReturn(false);

        MessageGraphImporter importer = new MessageGraphImporter(
                reactionObserverService, messageGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(messageGraphRepository).existsByMessageTypeNameAndVariant(messageTypeName, expectedVariant);
        verify(messageGraphRepository).save(argThat(saved -> expectedVariant.equals(saved.getVariant())));
    }
}
