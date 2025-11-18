package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.ConfluenceAdapter;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.NodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.system.ComponentGraph;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.ComponentGraphRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@AllArgsConstructor
@Slf4j
public class ComponentGraphAttachmentService {

    private final ComponentGraphRepository componentGraphRepository;
    private final GraphvizRenderer imageRenderer;
    private final ConfluenceAdapter confluenceAdapter;
    private final ObjectMapper objectMapper;

    public void generateAttachment(SystemComponent component, String pageId) {
        try {
            ComponentGraph graph = componentGraphRepository.findByComponentNameIgnoreCase(component.getName());
            if (graph == null) {
                log.info("No graph found for component {}", component.getName());
                return;
            }

            if (isOutdated(graph)) {
                log.info("Graph fingerprint changed since last generation. Updating graph image for component {}.", component.getName());
                InputStream imageStream = createNewGraphPng(graph);
                confluenceAdapter.addOrUpdateAttachment(pageId, getComponentAttachmentName(component.getName()), imageStream);
                componentGraphRepository.updateLastPublishedFingerprint(graph.getId(), graph.getFingerprint());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error generating graph attachment", e);
        }
    }

    private InputStream createNewGraphPng(ComponentGraph graph) throws IOException {
        GraphDto graphDto = objectMapper.readValue(graph.getGraphData(), GraphDto.class);
        highlightOtherSystemsMessageNodes(graphDto, graph.getSystemName());
        return imageRenderer.renderPng(graphDto);
    }

    void highlightOtherSystemsMessageNodes(GraphDto graph, String systemName) {
        for (NodeDto node : graph.getNodes()) {
            if (node instanceof MessageNodeDto msgNode &&
                    !msgNode.getMessageType().toLowerCase().startsWith(systemName.toLowerCase())) {
                msgNode.setHighlighted(true);
            }
        }
    }

    boolean isOutdated(ComponentGraph graph) {
        return !graph.getFingerprint().equals(graph.getLastPublishedFingerprint());
    }

    public String getComponentAttachmentNameIfExists(String componentName) {
        ComponentGraph graph = componentGraphRepository.findByComponentNameIgnoreCase(componentName);
        if (graph == null) {
            return null;
        }
        return getComponentAttachmentName(componentName);
    }

    public String getComponentAttachmentName(String componentName) {
        return "graph-" + componentName + ".png";
    }
}
