package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.importer.reaction.client.GraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemGraph;
import ch.admin.bit.jeap.archrepo.persistence.SystemGraphRepository;
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
class SystemGraphImporterTest {

    @Mock
    private ReactionObserverService reactionObserverService;

    @Mock
    private SystemGraphRepository systemGraphRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ArchitectureModel model;

    @Captor
    private ArgumentCaptor<SystemGraph> systemGraphCaptor;

    @Test
    void importIntoModel_withNewSystemGraph_savesSystemGraph() throws JsonProcessingException {
        // Arrange
        String systemName = "testSystem";
        when(reactionObserverService.getSystemNames()).thenReturn(List.of(systemName));
        when(systemGraphRepository.existsBySystemName(systemName)).thenReturn(false);

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
        when(reactionObserverService.getSystemGraph(systemName)).thenReturn(graphDto);

        String expectedSerializedData = "{\"nodes\":[{\"nodeType\":\"MESSAGE\",\"id\":123,\"messageKey\":\"msg_123\",\"messageType\":\"TestEvent\",\"variant\":\"default\"},{\"nodeType\":\"REACTION\",\"id\":77,\"component\":\"test-component\"}],\"edges\":[{\"edgeType\":\"TRIGGER\",\"sourceId\":123,\"sourceNodeType\":\"MESSAGE\",\"targetReactionId\":77,\"median\":10},{\"edgeType\":\"ACTION\",\"sourceReactionId\":77,\"targetId\":123,\"targetNodeType\":\"REACTION\"}]}";
        byte[] serializedData = expectedSerializedData.getBytes();
        when(objectMapper.writeValueAsBytes(graphData)).thenReturn(serializedData);

        SystemGraphImporter importer = new SystemGraphImporter(reactionObserverService, systemGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(systemGraphRepository).existsBySystemName(systemName);
        verify(systemGraphRepository).save(systemGraphCaptor.capture());
        verify(systemGraphRepository, never()).updateGraphDataAndFingerprintIfFingerprintChanged(any(), any(), any());

        SystemGraph capturedSystemGraph = systemGraphCaptor.getValue();
        assertThat(capturedSystemGraph.getSystemName()).isEqualTo(systemName);
        assertThat(capturedSystemGraph.getGraphData()).isEqualTo(serializedData);
        assertThat(capturedSystemGraph.getFingerprint()).isEqualTo("fingerprint123");
    }

    @Test
    void importIntoModel_withExistingSystemGraph_updatesSystemGraph() throws JsonProcessingException {
        // Arrange
        String systemName = "testSystem";
        when(reactionObserverService.getSystemNames()).thenReturn(List.of(systemName));
        when(systemGraphRepository.existsBySystemName(systemName)).thenReturn(true);

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
        when(reactionObserverService.getSystemGraph(systemName)).thenReturn(graphDto);

        String expectedSerializedData = "{\"nodes\":[{\"nodeType\":\"MESSAGE\",\"id\":124,\"messageKey\":\"msg_124\",\"messageType\":\"UpdatedEvent\",\"variant\":\"default\"},{\"nodeType\":\"REACTION\",\"id\":78,\"component\":\"updated-component\"}],\"edges\":[{\"edgeType\":\"TRIGGER\",\"sourceId\":124,\"sourceNodeType\":\"MESSAGE\",\"targetReactionId\":78,\"median\":15},{\"edgeType\":\"ACTION\",\"sourceReactionId\":78,\"targetId\":124,\"targetNodeType\":\"REACTION\"}]}";
        byte[] serializedData = expectedSerializedData.getBytes();
        when(objectMapper.writeValueAsBytes(graphData)).thenReturn(serializedData);

        SystemGraphImporter importer = new SystemGraphImporter(reactionObserverService, systemGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(systemGraphRepository).existsBySystemName(systemName);
        verify(systemGraphRepository).updateGraphDataAndFingerprintIfFingerprintChanged(systemName, serializedData, "fingerprint123");
        verify(systemGraphRepository, never()).save(any());
    }

    @Test
    void importIntoModel_withoutSystemGraphs_doesNotSaveAnything() {
        // Arrange
        String systemName = "testSystem";
        when(reactionObserverService.getSystemNames()).thenReturn(List.of(systemName));
        when(reactionObserverService.getSystemGraph(systemName)).thenReturn(null);

        SystemGraphImporter importer = new SystemGraphImporter(
                reactionObserverService, systemGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(systemGraphRepository, never()).save(any());
        verify(systemGraphRepository, never()).updateGraphDataAndFingerprintIfFingerprintChanged(any(), any(), any());
    }

    @Test
    void importIntoModel_withMultipleSystems_processesAllSystems() throws JsonProcessingException {
        // Arrange
        String system1 = "system1";
        String system2 = "system2";
        when(reactionObserverService.getSystemNames()).thenReturn(List.of(system1, system2));
        when(systemGraphRepository.existsBySystemName(system1)).thenReturn(false);
        when(systemGraphRepository.existsBySystemName(system2)).thenReturn(true);

        Map<String, Object> graphData1 = Map.of("nodes", List.of("node1"));
        Map<String, Object> graphData2 = Map.of("nodes", List.of("node2"));

        GraphDto graphDto1 = new GraphDto(graphData1, "fingerprint1");
        GraphDto graphDto2 = new GraphDto(graphData2, "fingerprint2");

        when(reactionObserverService.getSystemGraph(system1)).thenReturn(graphDto1);
        when(reactionObserverService.getSystemGraph(system2)).thenReturn(graphDto2);

        byte[] serializedData1 = "{\"nodes\":[\"node1\"]}".getBytes();
        byte[] serializedData2 = "{\"nodes\":[\"node2\"]}".getBytes();

        when(objectMapper.writeValueAsBytes(graphData1)).thenReturn(serializedData1);
        when(objectMapper.writeValueAsBytes(graphData2)).thenReturn(serializedData2);

        SystemGraphImporter importer = new SystemGraphImporter(
                reactionObserverService, systemGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(systemGraphRepository).existsBySystemName(system1);
        verify(systemGraphRepository).existsBySystemName(system2);
        verify(systemGraphRepository).save(any()); // system1 gets saved as new
        verify(systemGraphRepository).updateGraphDataAndFingerprintIfFingerprintChanged(system2, serializedData2, "fingerprint2"); // system2 gets updated
    }

    @Test
    void importIntoModel_withSerializationError_continuesProcessing() throws JsonProcessingException {
        // Arrange
        String systemName = "testSystem";
        when(reactionObserverService.getSystemNames()).thenReturn(List.of(systemName));

        Map<String, Object> graphData = Map.of("nodes", List.of("node1"));
        GraphDto graphDto = new GraphDto(graphData, "fingerprint123");
        when(reactionObserverService.getSystemGraph(systemName)).thenReturn(graphDto);

        when(objectMapper.writeValueAsBytes(graphData)).thenThrow(new JsonProcessingException("Serialization error") {});

        SystemGraphImporter importer = new SystemGraphImporter(
                reactionObserverService, systemGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(systemGraphRepository, never()).save(any());
        verify(systemGraphRepository, never()).updateGraphDataAndFingerprintIfFingerprintChanged(any(), any(), any());
    }

    @Test
    void importIntoModel_withNoSystems_doesNotSaveOrUpdate() {
        // Arrange
        when(reactionObserverService.getSystemNames()).thenReturn(Collections.emptyList());

        SystemGraphImporter importer = new SystemGraphImporter(
                reactionObserverService, systemGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(systemGraphRepository, never()).save(any());
        verify(systemGraphRepository, never()).updateGraphDataAndFingerprintIfFingerprintChanged(any(), any(), any());
    }
}
