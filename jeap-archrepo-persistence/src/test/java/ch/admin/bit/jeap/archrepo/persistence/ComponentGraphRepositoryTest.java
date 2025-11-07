package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.system.ComponentGraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class ComponentGraphRepositoryTest {

    @Autowired
    private ComponentGraphRepository componentGraphRepository;

    @Autowired
    private TestEntityManager entityManager;


    @Test
    void getMaxCreatedAndModifiedAtList_shouldReturnCorrectMaxValues() {
        // Arrange
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime earlier = now.minusDays(2);
        ZonedDateTime later = now.plusDays(1);

        ComponentGraph graph1 = ComponentGraph.builder()
                .systemName("system-a")
                .componentName("component-a")
                .graphData("graph-a".getBytes(StandardCharsets.UTF_8))
                .fingerprint("fp-a")
                .build();
        ReflectionTestUtils.setField(graph1, "createdAt", earlier);
        ReflectionTestUtils.setField(graph1, "modifiedAt", now);
        entityManager.persist(graph1);

        ComponentGraph graph2 = ComponentGraph.builder()
                .systemName("system-b")
                .componentName("component-b")
                .graphData("graph-b".getBytes(StandardCharsets.UTF_8))
                .fingerprint("fp-b")
                .build();
        ReflectionTestUtils.setField(graph2, "createdAt", later);
        ReflectionTestUtils.setField(graph2, "modifiedAt", now);
        entityManager.persist(graph2);

        entityManager.flush();

        // Act
        List<ReactionLastModifiedAt> result = componentGraphRepository.getMaxCreatedAndModifiedAtList();

        // Assert
        assertThat(result).hasSize(2);

        ReactionLastModifiedAt compA = result.stream()
                .filter(r -> r.getComponent().equals("component-a"))
                .findFirst()
                .orElseThrow();

        ReactionLastModifiedAt compB = result.stream()
                .filter(r -> r.getComponent().equals("component-b"))
                .findFirst()
                .orElseThrow();

        assertThat(compA.getMaxCreatedAt().toInstant()).isCloseTo(earlier.toInstant(), within(1, ChronoUnit.MILLIS));
        assertThat(compA.getMaxModifiedAt().toInstant()).isCloseTo(now.toInstant(), within(1, ChronoUnit.MILLIS));

        assertThat(compB.getMaxCreatedAt().toInstant()).isCloseTo(later.toInstant(), within(1, ChronoUnit.MILLIS));
        assertThat(compB.getMaxModifiedAt().toInstant()).isCloseTo(now.toInstant(), within(1, ChronoUnit.MILLIS));

    }

    @Test
    void saveComponentGraph_and_find() {
        // Arrange
        String systemName = "test-system";
        String componentName = "test-component";
        String graphJson = createTestGraphJson("service-a", "service-b");
        String fingerprint = "test-fingerprint-123";
        ComponentGraph componentGraph = createComponentGraph(systemName, componentName, graphJson, fingerprint);

        // Act
        ComponentGraph savedGraph = componentGraphRepository.saveAndFlush(componentGraph);

        // Assert
        assertThat(componentGraphRepository.findAll()).hasSize(1);
        assertThat(savedGraph.getSystemName()).isEqualTo(systemName);
        assertThat(savedGraph.getComponentName()).isEqualTo(componentName);
        assertThat(savedGraph.getFingerprint()).isEqualTo(fingerprint);
        assertThat(new String(savedGraph.getGraphData(), StandardCharsets.UTF_8)).isEqualTo(graphJson);
        assertThat(ReflectionTestUtils.getField(savedGraph, "createdAt")).isNotNull();
        assertThat(ReflectionTestUtils.getField(savedGraph, "modifiedAt")).isNull();
    }

    @Test
    void existsBySystemNameAndComponentName_found() {
        // Arrange
        String systemName = "existing-system";
        String componentName = "existing-component";
        ComponentGraph componentGraph = createComponentGraph(systemName, componentName, createTestGraphJson("node1"), "fingerprint1");
        componentGraphRepository.saveAndFlush(componentGraph);

        // Act
        boolean exists = componentGraphRepository.existsBySystemNameAndComponentName(systemName, componentName);

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsBySystemNameAndComponentName_notFound() {
        // Arrange & Act
        boolean exists = componentGraphRepository.existsBySystemNameAndComponentName("non-existent-system", "non-existent-component");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void existsBySystemNameAndComponentName_foundSystemButNotComponent() {
        // Arrange
        String systemName = "existing-system";
        String existingComponentName = "existing-component";
        ComponentGraph componentGraph = createComponentGraph(systemName, existingComponentName, createTestGraphJson("node1"), "fingerprint1");
        componentGraphRepository.saveAndFlush(componentGraph);

        // Act
        boolean exists = componentGraphRepository.existsBySystemNameAndComponentName(systemName, "non-existent-component");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void updateGraphAndFingerprintBySystemNameAndComponentName_existingComponentIfFingerprintChanged() {
        // Arrange
        String systemName = "update-test-system";
        String componentName = "update-test-component";
        String originalGraph = createTestGraphJson("original-node");
        String originalFingerprint = "original-fingerprint";
        ComponentGraph originalComponentGraph = createComponentGraph(systemName, componentName, originalGraph, originalFingerprint);
        componentGraphRepository.saveAndFlush(originalComponentGraph);

        String updatedGraph = createTestGraphJson("updated-node-1", "updated-node-2");
        String updatedFingerprint = "updated-fingerprint";
        byte[] updatedGraphData = updatedGraph.getBytes(StandardCharsets.UTF_8);

        // Act
        componentGraphRepository.updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(systemName, componentName, updatedGraphData, updatedFingerprint);
        entityManager.clear(); // Clear persistence context to force reload from database

        // Assert
        ComponentGraph updatedComponentGraph = componentGraphRepository.findAll().get(0);
        assertThat(updatedComponentGraph.getSystemName()).isEqualTo(systemName);
        assertThat(updatedComponentGraph.getComponentName()).isEqualTo(componentName);
        assertThat(updatedComponentGraph.getFingerprint()).isEqualTo(updatedFingerprint);
        assertThat(new String(updatedComponentGraph.getGraphData(), StandardCharsets.UTF_8)).isEqualTo(updatedGraph);
        ZonedDateTime createdAt = (ZonedDateTime) ReflectionTestUtils.getField(updatedComponentGraph, "createdAt");
        ZonedDateTime modifiedAt = (ZonedDateTime) ReflectionTestUtils.getField(updatedComponentGraph, "modifiedAt");
        assertThat(createdAt).isNotNull();
        assertThat(modifiedAt).isNotNull().isAfter(createdAt);
    }

    @Test
    void updateGraphAndFingerprintBySystemNameAndComponentName_sameFingerprint_noUpdateIfFingerprintChanged() {
        // Arrange
        String systemName = "fingerprint-test-system";
        String componentName = "fingerprint-test-component";
        String originalGraph = createTestGraphJson("original-service");
        String fingerprint = "same-fingerprint";
        ComponentGraph originalComponentGraph = createComponentGraph(systemName, componentName, originalGraph, fingerprint);
        componentGraphRepository.saveAndFlush(originalComponentGraph);

        String newGraph = createTestGraphJson("updated-service");
        byte[] newGraphData = newGraph.getBytes(StandardCharsets.UTF_8);

        // Act - try to update with same fingerprint
        componentGraphRepository.updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(systemName, componentName, newGraphData, fingerprint);
        entityManager.clear(); // Clear persistence context to force reload from database

        // Assert - no update should have occurred since fingerprint is the same
        ComponentGraph unchangedComponentGraph = componentGraphRepository.findAll().get(0);
        assertThat(unchangedComponentGraph.getSystemName()).isEqualTo(systemName);
        assertThat(unchangedComponentGraph.getComponentName()).isEqualTo(componentName);
        assertThat(unchangedComponentGraph.getFingerprint()).isEqualTo(fingerprint);
        assertThat(new String(unchangedComponentGraph.getGraphData(), StandardCharsets.UTF_8)).isEqualTo(originalGraph); // Should still be original graph

        ZonedDateTime finalCreatedAt = (ZonedDateTime) ReflectionTestUtils.getField(unchangedComponentGraph, "createdAt");
        ZonedDateTime finalModifiedAt = (ZonedDateTime) ReflectionTestUtils.getField(unchangedComponentGraph, "modifiedAt");
        assertThat(finalCreatedAt).isNotNull();
        assertThat(finalModifiedAt).isNull();
    }

    private ComponentGraph createComponentGraph(String systemName, String componentName, String graphJson, String fingerprint) {
        return ComponentGraph.builder()
                .systemName(systemName)
                .componentName(componentName)
                .graphData(graphJson.getBytes(StandardCharsets.UTF_8))
                .fingerprint(fingerprint)
                .build();
    }

    private String createTestGraphJson(String... componentNames) {
        StringBuilder json = new StringBuilder();
        json.append("{\"nodes\": [");

        // Create MESSAGE and REACTION nodes for each component
        boolean first = true;
        int nodeId = 100;
        for (String componentName : componentNames) {
            if (!first) json.append(", ");

            // MESSAGE node
            json.append("{")
                .append("\"nodeType\": \"MESSAGE\", ")
                .append("\"id\": ").append(nodeId).append(", ")
                .append("\"messageKey\": \"msg_").append(nodeId).append("\", ")
                .append("\"messageType\": \"").append(componentName).append("Event\", ")
                .append("\"variant\": \"default\"")
                .append("}");

            json.append(", ");

            // REACTION node
            int reactionId = nodeId + 1000;
            json.append("{")
                .append("\"nodeType\": \"REACTION\", ")
                .append("\"id\": ").append(reactionId).append(", ")
                .append("\"component\": \"").append(componentName).append("\"")
                .append("}");

            nodeId += 100;
            first = false;
        }

        json.append("], \"edges\": [");

        // Create TRIGGER and ACTION edges
        first = true;
        nodeId = 100;
        for (String componentName : componentNames) {
            if (!first) json.append(", ");

            int reactionId = nodeId + 1000;

            // TRIGGER edge
            json.append("{")
                .append("\"edgeType\": \"TRIGGER\", ")
                .append("\"sourceId\": ").append(nodeId).append(", ")
                .append("\"sourceNodeType\": \"MESSAGE\", ")
                .append("\"targetReactionId\": ").append(reactionId).append(", ")
                .append("\"median\": 10")
                .append("}");

            // ACTION edge
            json.append(", ");
            json.append("{")
                    .append("\"edgeType\": \"ACTION\", ")
                    .append("\"sourceReactionId\": ").append(reactionId).append(", ")
                    .append("\"targetId\": ").append(nodeId + 100).append(", ")
                    .append("\"targetNodeType\": \"MESSAGE\"")
                    .append("}");

            nodeId += 100;
            first = false;
        }

        json.append("]}");
        return json.toString();
    }
}
