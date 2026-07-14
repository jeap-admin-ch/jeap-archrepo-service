package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.NodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.system.ComponentGraph;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.ComponentGraphRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComponentGraphService {

    private final ComponentGraphRepository componentGraphRepository;
    private final ObjectMapper objectMapper;

    public RenderedReactionGraph getGraph(SystemComponent component, Function<NodeDto, String> linkResolver) {
        try {
            ComponentGraph graph = componentGraphRepository.findByComponentNameIgnoreCase(component.getName());
            if (graph == null) {
                log.info("No graph found for component {}", component.getName());
                return null;
            }
            GraphDto graphDto = objectMapper.readValue(graph.getGraphData(), GraphDto.class);
            highlightOtherSystemsMessageNodes(graphDto, graph.getSystemName());
            return new RenderedReactionGraph(component.getName(), graphDto.toDot(linkResolver));
        } catch (JacksonException e) {
            throw new RuntimeException("Error generating component graph", e);
        }
    }

    void highlightOtherSystemsMessageNodes(GraphDto graph, String systemName) {
        for (NodeDto node : graph.getNodes()) {
            if (node instanceof MessageNodeDto msgNode &&
                    !msgNode.getMessageType().toLowerCase().startsWith(systemName.toLowerCase())) {
                msgNode.setHighlighted(true);
            }
        }
    }
}
