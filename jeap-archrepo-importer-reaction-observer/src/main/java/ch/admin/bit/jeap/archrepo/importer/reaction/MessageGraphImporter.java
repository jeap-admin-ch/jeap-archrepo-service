package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.importer.reaction.client.GraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.MessageGraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import ch.admin.bit.jeap.archrepo.persistence.MessageGraphRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        for (var messageType : model.getAllMessageTypes()) {
            importMessageType(messageType.getMessageTypeName());
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
        messageGraphDto.getVariants().forEach(v -> {
            try {
                GraphDto graphDto = messageGraphDto.get(v);
                // 'No variant' is delivered as messageTypeName from Reaction Observer Service
                String variant;
                if (messageTypeName.equals(v)) {
                    variant = "";
                } else if (v.contains("/")) {
                    variant = v.substring(v.indexOf("/") + 1);
                } else {
                    variant = v;
                }
                byte[] graphData = objectMapper.writeValueAsBytes(graphDto.graph());

                if (messageGraphRepository.existsByMessageTypeNameAndVariant(messageTypeName, variant)) {
                    messageGraphRepository.updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(
                            messageTypeName, variant, graphData, graphDto.fingerprint());
                    log.trace("Updated graph for message type: {} with fingerprint: {}",
                            messageTypeName, graphDto.fingerprint());
                } else {
                    MessageGraph messageGraph = MessageGraph.builder()
                            .messageTypeName(messageTypeName)
                            .variant(variant)
                            .graphData(graphData)
                            .fingerprint(graphDto.fingerprint())
                            .build();
                    messageGraphRepository.save(messageGraph);
                    log.trace("Saved new graph for message type: {} with fingerprint: {}",
                            messageTypeName, graphDto.fingerprint());
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

    }

}
