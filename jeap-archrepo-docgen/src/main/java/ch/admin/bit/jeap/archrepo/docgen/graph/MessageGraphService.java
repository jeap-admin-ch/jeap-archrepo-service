package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.NodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.persistence.MessageGraphRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageGraphService {

    private final MessageGraphRepository messageGraphRepository;
    private final ObjectMapper objectMapper;

    public List<RenderedReactionGraph> getGraphs(MessageType message, Function<NodeDto, String> linkResolver) {
        try {
            List<MessageGraph> messageGraphs = messageGraphRepository.findAllByMessageTypeName(message.getMessageTypeName());
            if (messageGraphs.isEmpty()) {
                log.info("No graph found for message {}", message.getMessageTypeName());
                return List.of();
            }
            return messageGraphs.stream()
                    .sorted(java.util.Comparator.comparing(MessageGraph::getVariant))
                    .map(graph -> renderGraph(message, graph, linkResolver))
                    .toList();
        } catch (JacksonException e) {
            throw new RuntimeException("Error generating message graphs", e);
        }
    }

    private RenderedReactionGraph renderGraph(MessageType message, MessageGraph graph, Function<NodeDto, String> linkResolver) {
        GraphDto graphDto = objectMapper.readValue(graph.getGraphData(), GraphDto.class);
        highlightMessageNode(graphDto, message);
        String navigationKey = MessageGraph.normalizeVariant(graph.getMessageTypeName(), graph.getVariant());
        String title = Optional.of(navigationKey).filter(v -> !v.isBlank()).orElse("Default");
        return new RenderedReactionGraph(title, graphDto.toDot(linkResolver));
    }

    void highlightMessageNode(GraphDto graph, MessageType message) {
        for (NodeDto node : graph.getNodes()) {
            if (node instanceof MessageNodeDto msgNode &&
                    message.getMessageTypeName().equals(msgNode.getMessageType())) {
                msgNode.setHighlighted(true);
            }
        }
    }
}
