package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class MessageGraphRepositoryTest {

    @Autowired
    private MessageGraphRepository messageGraphRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveMessageGraph_and_find() {
        // Arrange
        String messageTypeName = "UserCreatedEvent";
        String graphJson = createTestGraphJson("notification-service", "email-service");
        String fingerprint = "test-fingerprint-123";
        String variant = "";
        MessageGraph messageGraph = createMessageGraph(messageTypeName, variant, graphJson, fingerprint);

        // Act
        MessageGraph savedGraph = messageGraphRepository.saveAndFlush(messageGraph);

        // Assert
        assertThat(messageGraphRepository.findAll()).hasSize(1);
        assertThat(savedGraph.getMessageTypeName()).isEqualTo(messageTypeName);
        assertThat(savedGraph.getFingerprint()).isEqualTo(fingerprint);
        assertThat(savedGraph.getVariant()).isEqualTo(variant);
        assertThat(new String(savedGraph.getGraphData(), StandardCharsets.UTF_8)).isEqualTo(graphJson);
        assertThat(ReflectionTestUtils.getField(savedGraph, "createdAt")).isNotNull();
        assertThat(ReflectionTestUtils.getField(savedGraph, "modifiedAt")).isNull();
    }

    @Test
    void saveMessageGraphWithVariant_and_find() {
        // Arrange
        String messageTypeName = "UserCreatedEvent";
        String graphJson = createTestGraphJson("notification-service", "email-service");
        String fingerprint = "test-fingerprint-123";
        String variant = "var1";
        MessageGraph messageGraph = createMessageGraph(messageTypeName, variant, graphJson, fingerprint);

        // Act
        MessageGraph savedGraph = messageGraphRepository.saveAndFlush(messageGraph);

        // Assert
        assertThat(messageGraphRepository.findAll()).hasSize(1);
        assertThat(savedGraph.getMessageTypeName()).isEqualTo(messageTypeName);
        assertThat(savedGraph.getFingerprint()).isEqualTo(fingerprint);
        assertThat(savedGraph.getVariant()).isEqualTo(variant);
        assertThat(new String(savedGraph.getGraphData(), StandardCharsets.UTF_8)).isEqualTo(graphJson);
        assertThat(ReflectionTestUtils.getField(savedGraph, "createdAt")).isNotNull();
        assertThat(ReflectionTestUtils.getField(savedGraph, "modifiedAt")).isNull();
    }

    @Test
    void existsByMessageTypeNameAndVariant_found() {
        // Arrange
        String messageTypeName = "ExistingEvent";
        String variant = "";
        MessageGraph messageGraph = createMessageGraph(messageTypeName, variant, createTestGraphJson("service1"), "fingerprint1");
        messageGraphRepository.saveAndFlush(messageGraph);

        // Act
        boolean exists = messageGraphRepository.existsByMessageTypeNameAndVariant(messageTypeName, variant);

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByMessageTypeNameAndVariantWithVariant_found() {
        // Arrange
        String messageTypeName = "ExistingEvent";
        String variant = "v1";
        MessageGraph messageGraph = createMessageGraph(messageTypeName, variant, createTestGraphJson("service1"), "fingerprint1");
        messageGraphRepository.saveAndFlush(messageGraph);

        // Act
        boolean exists = messageGraphRepository.existsByMessageTypeNameAndVariant(messageTypeName, variant);

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByMessageTypeNameAndVariant_notFound() {
        // Arrange
        String messageTypeName = "NonExistentEvent";
        String variant = "v1";

        // Act
        boolean exists = messageGraphRepository.existsByMessageTypeNameAndVariant(messageTypeName, variant);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void existsByMessageTypeNameAndVariant_VariantNotFound() {
        // Arrange
        String messageTypeName = "UserCreatedEvent";
        String graphJson = createTestGraphJson("notification-service", "email-service");
        String fingerprint = "test-fingerprint-123";
        String variant = "var1";
        MessageGraph messageGraph = createMessageGraph(messageTypeName, variant, graphJson, fingerprint);

        // Act
        messageGraphRepository.saveAndFlush(messageGraph);
        boolean exists = messageGraphRepository.existsByMessageTypeNameAndVariant(messageTypeName, "nonexisting-variant");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void updateGraphAndFingerprintByMessageTypeNameAndVariant_existingMessageIfFingerprintChanged() {
        // Arrange
        String messageTypeName = "UpdateTestEvent";
        String originalGraph = createTestGraphJson("original-service");
        String originalFingerprint = "original-fingerprint";
        String variant = "var1";
        MessageGraph originalMessageGraph = createMessageGraph(messageTypeName, variant, originalGraph, originalFingerprint);
        messageGraphRepository.saveAndFlush(originalMessageGraph);

        String updatedGraph = createTestGraphJson("updated-service-1", "updated-service-2");
        String updatedFingerprint = "updated-fingerprint";
        byte[] updatedGraphData = updatedGraph.getBytes(StandardCharsets.UTF_8);

        // Act
        messageGraphRepository.updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(messageTypeName, variant, updatedGraphData, updatedFingerprint);
        entityManager.clear(); // Clear persistence context to force reload from database

        // Assert
        MessageGraph updatedMessageGraph = messageGraphRepository.findAll().getFirst();
        assertThat(updatedMessageGraph.getMessageTypeName()).isEqualTo(messageTypeName);
        assertThat(updatedMessageGraph.getVariant()).isEqualTo(variant);
        assertThat(updatedMessageGraph.getFingerprint()).isEqualTo(updatedFingerprint);
        assertThat(new String(updatedMessageGraph.getGraphData(), StandardCharsets.UTF_8)).isEqualTo(updatedGraph);
        ZonedDateTime createdAt = (ZonedDateTime) ReflectionTestUtils.getField(updatedMessageGraph, "createdAt");
        ZonedDateTime modifiedAt = (ZonedDateTime) ReflectionTestUtils.getField(updatedMessageGraph, "modifiedAt");
        assertThat(createdAt).isNotNull();
        assertThat(modifiedAt).isNotNull().isAfter(createdAt);
    }

    @Test
    void updateGraphAndFingerprintByMessageTypeNameAndVariant_nonExistentMessage_noEffectIfFingerprintChanged() {
        // Arrange
        String nonExistentMessageTypeName = "NonExistentEvent";
        String nonExistingVariant = "nonexisting-variant";
        String graphJson = createTestGraphJson("some-service");
        byte[] graphData = graphJson.getBytes(StandardCharsets.UTF_8);
        String fingerprint = "some-fingerprint";

        // Act
        messageGraphRepository.updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(nonExistentMessageTypeName, nonExistingVariant, graphData, fingerprint);

        // Assert
        assertThat(messageGraphRepository.findAll()).isEmpty();
    }

    @Test
    void updateGraphAndFingerprintByMessageTypeName_sameFingerprint_noUpdateIfFingerprintChanged() {
        // Arrange
        String messageTypeName = "FingerprintTestEvent";
        String originalGraph = createTestGraphJson("original-service");
        String fingerprint = "same-fingerprint";
        String variant = "same-variant";
        MessageGraph originalMessageGraph = createMessageGraph(messageTypeName, variant, originalGraph, fingerprint);
        messageGraphRepository.saveAndFlush(originalMessageGraph);

        String newGraph = createTestGraphJson("updated-service");
        byte[] newGraphData = newGraph.getBytes(StandardCharsets.UTF_8);

        // Act - try to update with same fingerprint
        messageGraphRepository.updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(messageTypeName, variant, newGraphData, fingerprint);
        entityManager.clear(); // Clear persistence context to force reload from database

        // Assert - no update should have occurred since fingerprint is the same
        MessageGraph unchangedMessageGraph = messageGraphRepository.findAll().getFirst();
        assertThat(unchangedMessageGraph.getMessageTypeName()).isEqualTo(messageTypeName);
        assertThat(unchangedMessageGraph.getVariant()).isEqualTo(variant);
        assertThat(unchangedMessageGraph.getFingerprint()).isEqualTo(fingerprint);
        assertThat(new String(unchangedMessageGraph.getGraphData(), StandardCharsets.UTF_8)).isEqualTo(originalGraph); // Should still be original graph

        ZonedDateTime finalCreatedAt = (ZonedDateTime) ReflectionTestUtils.getField(unchangedMessageGraph, "createdAt");
        ZonedDateTime finalModifiedAt = (ZonedDateTime) ReflectionTestUtils.getField(unchangedMessageGraph, "modifiedAt");
        assertThat(finalCreatedAt).isNotNull();
        assertThat(finalModifiedAt).isNull();
    }

    @Test
    void saveMultipleMessageGraphs_withDifferentMessageTypes() {
        // Arrange
        String variant = "variant";
        MessageGraph graph1 = createMessageGraph("Event1", variant, createTestGraphJson("service1"), "fingerprint1");
        MessageGraph graph2 = createMessageGraph("Event2", variant, createTestGraphJson("service2"), "fingerprint2");

        // Act
        messageGraphRepository.saveAndFlush(graph1);
        messageGraphRepository.saveAndFlush(graph2);

        // Assert
        assertThat(messageGraphRepository.findAll()).hasSize(2);
        assertThat(messageGraphRepository.existsByMessageTypeNameAndVariant("Event1", variant)).isTrue();
        assertThat(messageGraphRepository.existsByMessageTypeNameAndVariant("Event2", variant)).isTrue();
        assertThat(messageGraphRepository.existsByMessageTypeNameAndVariant("Event3", variant)).isFalse();
    }

    private MessageGraph createMessageGraph(String messageTypeName, String variant, String graphJson, String fingerprint) {
        return MessageGraph.builder()
                .messageTypeName(messageTypeName)
                .variant(variant)
                .graphData(graphJson.getBytes(StandardCharsets.UTF_8))
                .fingerprint(fingerprint)
                .build();
    }

    private String createTestGraphJson(String... serviceNames) {
        StringBuilder json = new StringBuilder();
        json.append("{\"nodes\": [");

        // Create MESSAGE and REACTION nodes for each service
        boolean first = true;
        int nodeId = 100;
        for (String serviceName : serviceNames) {
            if (!first) json.append(", ");

            // MESSAGE node
            json.append("{")
                .append("\"nodeType\": \"MESSAGE\", ")
                .append("\"id\": ").append(nodeId).append(", ")
                .append("\"messageKey\": \"msg_").append(nodeId).append("\", ")
                .append("\"messageType\": \"").append(serviceName).append("Event\", ")
                .append("\"variant\": \"default\"")
                .append("}");

            json.append(", ");

            // REACTION node
            int reactionId = nodeId + 1000;
            json.append("{")
                .append("\"nodeType\": \"REACTION\", ")
                .append("\"id\": ").append(reactionId).append(", ")
                .append("\"component\": \"").append(serviceName).append("\"")
                .append("}");

            nodeId += 100;
            first = false;
        }

        json.append("], \"edges\": [");

        // Create TRIGGER and ACTION edges
        first = true;
        nodeId = 100;
        for (String serviceName : serviceNames) {
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
