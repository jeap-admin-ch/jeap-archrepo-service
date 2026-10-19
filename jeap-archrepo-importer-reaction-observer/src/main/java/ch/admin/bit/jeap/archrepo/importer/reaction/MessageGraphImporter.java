package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.importer.reaction.client.GraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.MessageGraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.persistence.MessageGraphRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
class MessageGraphImporter implements ArchRepoImporter {

    private final ReactionObserverService reactionObserverService;
    private final MessageGraphRepository messageGraphRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void importIntoModel(ArchitectureModel model, String environment) {
        log.info("Getting message graphs from Reaction Observer Service...");
        List<MessageType> messageTypes = model.getAllMessageTypes();
        for (var messageType : messageTypes) {
            importMessageType(messageType.getMessageTypeName());
        }
        Set<String> messageTypeNames = messageTypes.stream()
                .map(MessageType::getMessageTypeName)
                .collect(Collectors.toSet());
        if (messageTypeNames.isEmpty()) {
            messageGraphRepository.deleteAllMessageGraphs();
        } else {
            messageGraphRepository.deleteGraphsForMissingMessageTypes(messageTypeNames);
        }
        log.info("Message graph import completed");
    }

    private void importMessageType(String messageTypeName) {
        log.trace("Processing message type: {}", messageTypeName);
        try {
            MessageGraphDto graphDto = reactionObserverService.getMessageGraph(messageTypeName);
            if (graphDto == null) {
                log.warn("No graph data found for message type: {}", messageTypeName);
                return;
            }
            saveOrUpdateMessageGraph(messageTypeName, graphDto);
        } catch (Exception e) {
            log.warn("Failed to get or process graph data for message type: {}", messageTypeName, e);
        }
    }

    private void saveOrUpdateMessageGraph(String messageTypeName, MessageGraphDto messageGraphDto) throws Exception {
        Map<String, SerializedGraph> graphsByVariant = new LinkedHashMap<>();
        List<String> variants = messageGraphDto.getVariants().stream()
                .sorted(Comparator
                        .comparingInt((String variant) -> variantPrecedence(messageTypeName, variant))
                        .thenComparing(String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Comparator.naturalOrder()))
                .toList();
        for (String rawVariant : variants) {
            GraphDto graphDto = messageGraphDto.get(rawVariant);
            String variant = MessageGraph.normalizeVariant(messageTypeName, rawVariant);
            SerializedGraph graph = new SerializedGraph(objectMapper.writeValueAsBytes(graphDto.graph()), graphDto.fingerprint());
            SerializedGraph previous = graphsByVariant.putIfAbsent(variant, graph);
            if (previous != null && !previous.equals(graph)) {
                log.warn("Ignoring conflicting message graph alias '{}' for message type '{}' and normalized variant '{}'",
                        rawVariant, messageTypeName, variant);
            }
        }

        for (Map.Entry<String, SerializedGraph> entry : graphsByVariant.entrySet()) {
            String variant = entry.getKey();
            SerializedGraph graph = entry.getValue();
            if (messageGraphRepository.existsByMessageTypeNameAndVariant(messageTypeName, variant)) {
                messageGraphRepository.updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(
                        messageTypeName, variant, graph.data(), graph.fingerprint());
                log.trace("Updated graph for message type: {} with fingerprint: {}",
                        messageTypeName, graph.fingerprint());
            } else {
                MessageGraph messageGraph = MessageGraph.builder()
                        .messageTypeName(messageTypeName)
                        .variant(variant)
                        .graphData(graph.data())
                        .fingerprint(graph.fingerprint())
                        .build();
                messageGraphRepository.save(messageGraph);
                log.trace("Saved new graph for message type: {} with fingerprint: {}",
                        messageTypeName, graph.fingerprint());
            }
        }

        if (graphsByVariant.isEmpty()) {
            messageGraphRepository.deleteAllVariants(messageTypeName);
        } else {
            messageGraphRepository.deleteStaleVariants(messageTypeName, graphsByVariant.keySet());
        }
    }

    record SerializedGraph(byte[] data, String fingerprint) {

        @Override
        public boolean equals(Object other) {
            return other instanceof SerializedGraph graph &&
                    Arrays.equals(data, graph.data) && Objects.equals(fingerprint, graph.fingerprint);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(data) + Objects.hashCode(fingerprint);
        }

        @Override
        public String toString() {
            return "SerializedGraph[data=" + Arrays.toString(data) + ", fingerprint=" + fingerprint + ']';
        }
    }

    private static int variantPrecedence(String messageTypeName, String rawVariant) {
        String trimmed = rawVariant == null ? "" : rawVariant.trim();
        String normalized = MessageGraph.normalizeVariant(messageTypeName, rawVariant);
        if (trimmed.equals(normalized)) {
            return 0;
        }
        if (trimmed.equalsIgnoreCase(messageTypeName)) {
            return 1;
        }
        if ("default".equalsIgnoreCase(trimmed)) {
            return 2;
        }
        return 3;
    }
}
