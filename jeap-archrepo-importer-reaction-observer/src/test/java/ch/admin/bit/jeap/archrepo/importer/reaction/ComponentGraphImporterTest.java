package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.importer.reaction.client.GraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.system.ComponentGraph;
import ch.admin.bit.jeap.archrepo.persistence.ComponentGraphRepository;
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
class ComponentGraphImporterTest {

    @Mock
    private ReactionObserverService reactionObserverService;

    @Mock
    private ComponentGraphRepository componentGraphRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ArchitectureModel model;

    @Captor
    private ArgumentCaptor<ComponentGraph> componentGraphCaptor;

    @Test
    void importIntoModel_withNewServiceGraph_savesComponentGraph() throws JsonProcessingException {
        // Arrange
        String systemName = "testSystem";
        String componentName = "testComponent";
        Map<String, String> componentToSystemMap = Map.of(componentName, systemName);
        when(model.getAllSystemComponentNamesWithSystemName()).thenReturn(componentToSystemMap);
        when(componentGraphRepository.existsBySystemNameAndComponentName(systemName, componentName)).thenReturn(false);

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
        when(reactionObserverService.getComponentGraph(componentName)).thenReturn(graphDto);

        String expectedSerializedData = "{\"nodes\":[{\"nodeType\":\"MESSAGE\",\"id\":123,\"messageKey\":\"msg_123\",\"messageType\":\"TestEvent\",\"variant\":\"default\"},{\"nodeType\":\"REACTION\",\"id\":77,\"component\":\"test-component\"}],\"edges\":[{\"edgeType\":\"TRIGGER\",\"sourceId\":123,\"sourceNodeType\":\"MESSAGE\",\"targetReactionId\":77,\"median\":10},{\"edgeType\":\"ACTION\",\"sourceReactionId\":77,\"targetId\":123,\"targetNodeType\":\"REACTION\"}]}";
        byte[] serializedData = expectedSerializedData.getBytes();
        when(objectMapper.writeValueAsBytes(graphData)).thenReturn(serializedData);

        ComponentGraphImporter importer = new ComponentGraphImporter(reactionObserverService, componentGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(componentGraphRepository).existsBySystemNameAndComponentName(systemName, componentName);
        verify(componentGraphRepository).save(componentGraphCaptor.capture());
        verify(componentGraphRepository, never()).updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(any(), any(), any(), any());

        ComponentGraph capturedComponentGraph = componentGraphCaptor.getValue();
        assertThat(capturedComponentGraph.getSystemName()).isEqualTo(systemName);
        assertThat(capturedComponentGraph.getComponentName()).isEqualTo(componentName);
        assertThat(capturedComponentGraph.getGraphData()).isEqualTo(serializedData);
        assertThat(capturedComponentGraph.getFingerprint()).isEqualTo("fingerprint123");
    }

    @Test
    void importIntoModel_withExistingServiceGraph_updatesComponentGraph() throws JsonProcessingException {
        // Arrange
        String systemName = "testSystem";
        String componentName = "testComponent";
        Map<String, String> componentToSystemMap = Map.of(componentName, systemName);
        when(model.getAllSystemComponentNamesWithSystemName()).thenReturn(componentToSystemMap);
        when(componentGraphRepository.existsBySystemNameAndComponentName(systemName, componentName)).thenReturn(true);

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
        when(reactionObserverService.getComponentGraph(componentName)).thenReturn(graphDto);

        String expectedSerializedData = "{\"nodes\":[{\"nodeType\":\"MESSAGE\",\"id\":124,\"messageKey\":\"msg_124\",\"messageType\":\"UpdatedEvent\",\"variant\":\"default\"},{\"nodeType\":\"REACTION\",\"id\":78,\"component\":\"updated-component\"}],\"edges\":[{\"edgeType\":\"TRIGGER\",\"sourceId\":124,\"sourceNodeType\":\"MESSAGE\",\"targetReactionId\":78,\"median\":15},{\"edgeType\":\"ACTION\",\"sourceReactionId\":78,\"targetId\":124,\"targetNodeType\":\"REACTION\"}]}";
        byte[] serializedData = expectedSerializedData.getBytes();
        when(objectMapper.writeValueAsBytes(graphData)).thenReturn(serializedData);

        ComponentGraphImporter importer = new ComponentGraphImporter(reactionObserverService, componentGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(componentGraphRepository).existsBySystemNameAndComponentName(systemName, componentName);
        verify(componentGraphRepository).updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(systemName, componentName, serializedData, "fingerprint123");
        verify(componentGraphRepository, never()).save(any());
    }

    @Test
    void importIntoModel_withoutServiceGraphs_doesNotSaveAnything() {
        // Arrange
        String systemName = "testSystem";
        String componentName = "testComponent";
        Map<String, String> componentToSystemMap = Map.of(componentName, systemName);
        when(model.getAllSystemComponentNamesWithSystemName()).thenReturn(componentToSystemMap);
        when(reactionObserverService.getComponentGraph(componentName)).thenReturn(null);

        ComponentGraphImporter importer = new ComponentGraphImporter(
                reactionObserverService, componentGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(componentGraphRepository, never()).save(any());
        verify(componentGraphRepository, never()).updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(any(), any(), any(), any());
    }

    @Test
    void importIntoModel_withMultipleComponents_processesAllComponents() throws JsonProcessingException {
        // Arrange
        String system1 = "system1";
        String system2 = "system2";
        String component1 = "component1";
        String component2 = "component2";
        Map<String, String> componentToSystemMap = Map.of(component1, system1, component2, system2);
        when(model.getAllSystemComponentNamesWithSystemName()).thenReturn(componentToSystemMap);
        when(componentGraphRepository.existsBySystemNameAndComponentName(system1, component1)).thenReturn(false);
        when(componentGraphRepository.existsBySystemNameAndComponentName(system2, component2)).thenReturn(true);

        Map<String, Object> graphData1 = Map.of("nodes", List.of("node1"));
        Map<String, Object> graphData2 = Map.of("nodes", List.of("node2"));

        GraphDto graphDto1 = new GraphDto(graphData1, "fingerprint1");
        GraphDto graphDto2 = new GraphDto(graphData2, "fingerprint2");

        when(reactionObserverService.getComponentGraph(component1)).thenReturn(graphDto1);
        when(reactionObserverService.getComponentGraph(component2)).thenReturn(graphDto2);

        byte[] serializedData1 = "{\"nodes\":[\"node1\"]}".getBytes();
        byte[] serializedData2 = "{\"nodes\":[\"node2\"]}".getBytes();

        when(objectMapper.writeValueAsBytes(graphData1)).thenReturn(serializedData1);
        when(objectMapper.writeValueAsBytes(graphData2)).thenReturn(serializedData2);

        ComponentGraphImporter importer = new ComponentGraphImporter(
                reactionObserverService, componentGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(componentGraphRepository).existsBySystemNameAndComponentName(system1, component1);
        verify(componentGraphRepository).existsBySystemNameAndComponentName(system2, component2);
        verify(componentGraphRepository).save(any()); // component1 gets saved as new
        verify(componentGraphRepository).updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(system2, component2, serializedData2, "fingerprint2"); // component2 gets updated
    }

    @Test
    void importIntoModel_withSerializationError_continuesProcessing() throws JsonProcessingException {
        // Arrange
        String systemName = "testSystem";
        String componentName = "testComponent";
        Map<String, String> componentToSystemMap = Map.of(componentName, systemName);
        when(model.getAllSystemComponentNamesWithSystemName()).thenReturn(componentToSystemMap);

        Map<String, Object> graphData = Map.of("nodes", List.of("node1"));
        GraphDto graphDto = new GraphDto(graphData, "fingerprint123");
        when(reactionObserverService.getComponentGraph(componentName)).thenReturn(graphDto);

        when(objectMapper.writeValueAsBytes(graphData)).thenThrow(new JsonProcessingException("Serialization error") {});

        ComponentGraphImporter importer = new ComponentGraphImporter(
                reactionObserverService, componentGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(componentGraphRepository, never()).save(any());
        verify(componentGraphRepository, never()).updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(any(), any(), any(), any());
    }

    @Test
    void importIntoModel_withNoComponents_doesNotSaveOrUpdate() {
        // Arrange
        when(model.getAllSystemComponentNamesWithSystemName()).thenReturn(Collections.emptyMap());

        ComponentGraphImporter importer = new ComponentGraphImporter(
                reactionObserverService, componentGraphRepository, objectMapper);

        // Act
        importer.importIntoModel(model, "ref");

        // Assert
        verify(componentGraphRepository, never()).save(any());
        verify(componentGraphRepository, never()).updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(any(), any(), any(), any());
    }
}
