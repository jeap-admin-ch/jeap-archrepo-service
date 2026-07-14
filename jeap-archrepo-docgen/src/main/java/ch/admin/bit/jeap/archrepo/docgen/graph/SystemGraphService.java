package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.NodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemGraph;
import ch.admin.bit.jeap.archrepo.persistence.SystemGraphRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemGraphService {

    private final SystemGraphRepository systemGraphRepository;
    private final ObjectMapper objectMapper;

    public RenderedReactionGraph getGraph(System system, Function<NodeDto, String> linkResolver) {
        try {
            SystemGraph graph = systemGraphRepository.findBySystemNameIgnoreCase(system.getName());
            if (graph == null) {
                log.info("No graph found for system {}", system.getName());
                return null;
            }
            GraphDto graphDto = objectMapper.readValue(graph.getGraphData(), GraphDto.class);
            highlightOtherSystemsMessageNodes(graphDto, system);
            return new RenderedReactionGraph(system.getName(), graphDto.toDot(linkResolver));
        } catch (JacksonException e) {
            throw new RuntimeException("Error generating system graph", e);
        }
    }

    void highlightOtherSystemsMessageNodes(GraphDto graph, System system) {
        for (NodeDto node : graph.getNodes()) {
            if (node instanceof MessageNodeDto msgNode &&
                    !msgNode.getMessageType().toLowerCase().startsWith(system.getName().toLowerCase())) {
                msgNode.setHighlighted(true);
            }
        }
    }
}
