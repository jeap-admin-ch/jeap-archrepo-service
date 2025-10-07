package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.ConfluenceAdapter;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.NodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.persistence.MessageGraphRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class MessageGraphAttachmentService {

    private final MessageGraphRepository messageGraphRepository;
    private final GraphvizRenderer imageRenderer;
    private final ConfluenceAdapter confluenceAdapter;
    private final ObjectMapper objectMapper;

    public void generateAttachments(MessageType message, String pageId) {
        try {
            List<MessageGraph> messageGraphs = messageGraphRepository.findAllByMessageTypeName(message.getMessageTypeName());
            if (messageGraphs.isEmpty()) {
                log.info("No graph found for message {}", message.getMessageTypeName());
                return;
            }

            List<MessageGraph> outdatedGraphs = filterOutdatedGraphs(messageGraphs);
            for (MessageGraph graph : outdatedGraphs) {
                log.info("Graph fingerprint changed since last generation. Updating graph image for message {} with variant {}.",
                        message.getMessageTypeName(), Optional.ofNullable(graph.getVariant()).filter(v -> !v.isBlank()).orElse("not set"));
                InputStream imageStream = createNewGraphPng(message, graph);
                confluenceAdapter.addOrUpdateAttachment(pageId, generateMessageAttachmentName(graph), imageStream);
                messageGraphRepository.updateActualDocFingerprint(graph.getId(), graph.getFingerprint());
            }
            deleteUnusedGraphAttachments(pageId, messageGraphs);
        } catch (IOException e) {
            throw new RuntimeException("Error generating graph attachments", e);
        }
    }

    private void deleteUnusedGraphAttachments(String pageId, List<MessageGraph> messageGraphs) {
        List<String> attachmentNames = messageGraphs.stream()
                .map(this::generateMessageAttachmentName)
                .toList();
        confluenceAdapter.deleteUnusedAttachments(pageId, attachmentNames);
    }

    private InputStream createNewGraphPng(MessageType message, MessageGraph graph) throws IOException {
        GraphDto graphDto = objectMapper.readValue(graph.getGraphData(), GraphDto.class);
        highlightMessageNode(graphDto, message);
        return imageRenderer.renderPng(graphDto);
    }

    public List<String> getAttachmentNames(MessageType message) {
        List<MessageGraph> messageGraphs = messageGraphRepository.findAllByMessageTypeName(message.getMessageTypeName());
        return messageGraphs.stream()
                .map(this::generateMessageAttachmentName)
                .toList();
    }

    List<MessageGraph> filterOutdatedGraphs(List<MessageGraph> graphs) {
        return graphs.stream()
                .filter(graph -> !graph.getFingerprint().equals(graph.getActualDocFingerprint()))
                .toList();
    }

    void highlightMessageNode(GraphDto graph, MessageType message) {
        for (NodeDto node : graph.getNodes()) {
            if (node instanceof MessageNodeDto msgNode &&
                    message.getMessageTypeName().equals(msgNode.getMessageType())) {
                msgNode.setHighlighted(true);
            }
        }
    }

    String generateMessageAttachmentName(MessageGraph graph) {
        String base = "graph-" + graph.getMessageTypeName();
        return graph.getVariant().isBlank() ? base + ".png" : base + "-" + graph.getVariant() + ".png";
    }
}
