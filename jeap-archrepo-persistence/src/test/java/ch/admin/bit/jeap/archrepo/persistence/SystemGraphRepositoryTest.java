package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.system.SystemGraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class SystemGraphRepositoryTest {

    @Autowired
    private SystemGraphRepository systemGraphRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveSystemGraph_and_find() {
        // Arrange
        String systemName = "test-system";
        String graphJson = createTestGraphJson("service-a", "service-b");
        String fingerprint = "test-fingerprint-123";
        SystemGraph systemGraph = createSystemGraph(systemName, graphJson, fingerprint);

        // Act
        SystemGraph savedGraph = systemGraphRepository.saveAndFlush(systemGraph);

        // Assert
        assertThat(systemGraphRepository.findAll()).hasSize(1);
        assertThat(savedGraph.getSystemName()).isEqualTo(systemName);
        assertThat(savedGraph.getFingerprint()).isEqualTo(fingerprint);
        assertThat(new String(savedGraph.getGraphData(), StandardCharsets.UTF_8)).isEqualTo(graphJson);
        assertThat(ReflectionTestUtils.getField(savedGraph, "createdAt")).isNotNull();
        assertThat(ReflectionTestUtils.getField(savedGraph, "modifiedAt")).isNull();
    }

    @Test
    void existsBySystemName_found() {
        // Arrange
        String systemName = "existing-system";
        SystemGraph systemGraph = createSystemGraph(systemName, createTestGraphJson("node1"), "fingerprint1");
        systemGraphRepository.saveAndFlush(systemGraph);

        // Act
        boolean exists = systemGraphRepository.existsBySystemName(systemName);

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsBySystemName_notFound() {
        // Arrange
        String systemName = "non-existent-system";

        // Act
        boolean exists = systemGraphRepository.existsBySystemName(systemName);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void updateGraphDataAndFingerprintIfFingerprint_Changed_existingSystem() {
        // Arrange
        String systemName = "update-test-system";
        String originalGraph = createTestGraphJson("original-node");
        String originalFingerprint = "original-fingerprint";
        SystemGraph originalSystemGraph = createSystemGraph(systemName, originalGraph, originalFingerprint);
        systemGraphRepository.saveAndFlush(originalSystemGraph);

        String updatedGraph = createTestGraphJson("updated-node-1", "updated-node-2");
        String updatedFingerprint = "updated-fingerprint";
        byte[] updatedGraphData = updatedGraph.getBytes(StandardCharsets.UTF_8);

        // Act
        systemGraphRepository.updateGraphDataAndFingerprintIfFingerprintChanged(systemName, updatedGraphData, updatedFingerprint);
        entityManager.clear(); // Clear persistence context to force reload from database

        // Assert
        SystemGraph updatedSystemGraph = systemGraphRepository.findAll().get(0);
        assertThat(updatedSystemGraph.getSystemName()).isEqualTo(systemName);
        assertThat(updatedSystemGraph.getFingerprint()).isEqualTo(updatedFingerprint);
        assertThat(new String(updatedSystemGraph.getGraphData(), StandardCharsets.UTF_8)).isEqualTo(updatedGraph);
        ZonedDateTime createdAt = (ZonedDateTime) ReflectionTestUtils.getField(updatedSystemGraph, "createdAt");
        ZonedDateTime modifiedAt = (ZonedDateTime) ReflectionTestUtils.getField(updatedSystemGraph, "modifiedAt");
        assertThat(createdAt).isNotNull();
        assertThat(modifiedAt).isNotNull().isAfter(createdAt);
    }

    @Test
    void updateGraphDataAndFingerprint_sameFingerprint_noUpdateFingerprintIf() {
        // Arrange
        String systemName = "fingerprint-test-system";
        String originalGraph = createTestGraphJson("original-component");
        String fingerprint = "same-fingerprint";
        SystemGraph originalSystemGraph = createSystemGraph(systemName, originalGraph, fingerprint);
        systemGraphRepository.saveAndFlush(originalSystemGraph);

        String newGraph = createTestGraphJson("updated-component");
        byte[] newGraphData = newGraph.getBytes(StandardCharsets.UTF_8);

        // Act - try to update with same fingerprint
        systemGraphRepository.updateGraphDataAndFingerprintIfFingerprintChanged(systemName, newGraphData, fingerprint);
        entityManager.clear(); // Clear persistence context to force reload from database

        // Assert - no update should have occurred since fingerprint is the same
        SystemGraph unchangedSystemGraph = systemGraphRepository.findAll().get(0);
        assertThat(unchangedSystemGraph.getSystemName()).isEqualTo(systemName);
        assertThat(unchangedSystemGraph.getFingerprint()).isEqualTo(fingerprint);
        assertThat(new String(unchangedSystemGraph.getGraphData(), StandardCharsets.UTF_8)).isEqualTo(originalGraph); // Should still be original graph

        ZonedDateTime finalCreatedAt = (ZonedDateTime) ReflectionTestUtils.getField(unchangedSystemGraph, "createdAt");
        ZonedDateTime finalModifiedAt = (ZonedDateTime) ReflectionTestUtils.getField(unchangedSystemGraph, "modifiedAt");
        assertThat(finalCreatedAt).isNotNull();
        assertThat(finalModifiedAt).isNull();
    }

    private SystemGraph createSystemGraph(String systemName, String graphJson, String fingerprint) {
        return SystemGraph.builder()
                .systemName(systemName)
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
