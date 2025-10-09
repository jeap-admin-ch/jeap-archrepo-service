package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.ConfluenceAdapter;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.NodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemGraph;
import ch.admin.bit.jeap.archrepo.persistence.SystemGraphRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@AllArgsConstructor
@Slf4j
public class SystemGraphAttachmentService {

    private final SystemGraphRepository systemGraphRepository;
    private final GraphvizRenderer imageRenderer;
    private final ConfluenceAdapter confluenceAdapter;
    private final ObjectMapper objectMapper;

    public void generateAttachment(System system, String pageId) {
        try {
            SystemGraph graph = systemGraphRepository.findBySystemNameIgnoreCase(system.getName());
            if (graph == null) {
                log.info("No graph found for system {}", system.getName());
                return;
            }

            if (isOutdated(graph)) {
                log.info("Graph fingerprint changed since last generation. Updating graph image for system {}.", system.getName());
                InputStream imageStream = createNewGraphPng(system, graph);
                confluenceAdapter.addOrUpdateAttachment(pageId, getSystemAttachmentName(system.getName()), imageStream);
                systemGraphRepository.updateLastPublishedFingerprint(graph.getId(), graph.getFingerprint());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error generating graph attachment", e);
        }
    }

    private InputStream createNewGraphPng(System system, SystemGraph graph) throws IOException {
        GraphDto graphDto = objectMapper.readValue(graph.getGraphData(), GraphDto.class);
        highlightOtherSystemsMessageNodes(graphDto, system);
        return imageRenderer.renderPng(graphDto);
    }

    boolean isOutdated(SystemGraph graph) {
        return !graph.getFingerprint().equals(graph.getLastPublishedFingerprint());
    }

    void highlightOtherSystemsMessageNodes(GraphDto graph, System system) {
        for (NodeDto node : graph.getNodes()) {
            if (node instanceof MessageNodeDto msgNode &&
                    !msgNode.getMessageType().toLowerCase().startsWith(system.getName().toLowerCase())) {
                msgNode.setHighlighted(true);
            }
        }
    }

    public String getSystemAttachmentNameIfExists(String systemName) {
        SystemGraph graph = systemGraphRepository.findBySystemNameIgnoreCase(systemName);
        if (graph == null) {
            return null;
        }
        return getSystemAttachmentName(systemName);
    }

    public String getSystemAttachmentName(String systemName) {
        return "graph-" + systemName + ".png";
    }
}
