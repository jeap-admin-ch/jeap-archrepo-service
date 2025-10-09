package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.ConfluenceAdapter;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemGraph;
import ch.admin.bit.jeap.archrepo.persistence.SystemGraphRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemGraphAttachmentServiceTest {

    @Test
    void generateAttachment_shouldRenderAndUploadGraph_whenGraphIsOutdated() throws Exception {
        String systemName = "TestSystem";
        String pageId = "12345";
        String fingerprint = "new-fingerprint";
        String oldFingerprint = "old-fingerprint";

        System system = mock(System.class);
        when(system.getName()).thenReturn(systemName);

        SystemGraph graph = mock(SystemGraph.class);
        when(graph.getFingerprint()).thenReturn(fingerprint);
        when(graph.getLastPublishedFingerprint()).thenReturn(oldFingerprint);
        when(graph.getGraphData()).thenReturn("{\"nodes\":[],\"edges\":[]}".getBytes());

        SystemGraphRepository graphRepo = mock(SystemGraphRepository.class);
        when(graphRepo.findBySystemNameIgnoreCase(systemName)).thenReturn(graph);

        GraphDto graphDto = mock(GraphDto.class);
        when(graphDto.getNodes()).thenReturn(List.of()); // keine Nodes zum Highlighten

        GraphvizRenderer renderer = mock(GraphvizRenderer.class);
        InputStream dummyImageStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(renderer.renderPng(any())).thenReturn(dummyImageStream);

        ConfluenceAdapter confluenceAdapter = mock(ConfluenceAdapter.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(any(byte[].class), eq(GraphDto.class))).thenReturn(graphDto);

        SystemGraphAttachmentService service = new SystemGraphAttachmentService(
                graphRepo, renderer, confluenceAdapter, objectMapper
        );

        service.generateAttachment(system, pageId);

        verify(renderer).renderPng(graphDto);
        verify(confluenceAdapter).addOrUpdateAttachment(eq(pageId), eq("graph-TestSystem.png"), eq(dummyImageStream));
        verify(graphRepo).updateLastPublishedFingerprint(graph.getId(), fingerprint);
    }

    @Test
    void generateAttachment_shouldDoNothing_whenNoGraphExists() {
        System system = mock(System.class);
        when(system.getName()).thenReturn("MissingSystem");

        SystemGraphRepository graphRepo = mock(SystemGraphRepository.class);
        when(graphRepo.findBySystemNameIgnoreCase("MissingSystem")).thenReturn(null);

        GraphvizRenderer renderer = mock(GraphvizRenderer.class);
        ConfluenceAdapter confluenceAdapter = mock(ConfluenceAdapter.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);

        SystemGraphAttachmentService service = new SystemGraphAttachmentService(
                graphRepo, renderer, confluenceAdapter, objectMapper
        );

        service.generateAttachment(system, "pageId");

        verifyNoInteractions(renderer);
        verifyNoInteractions(confluenceAdapter);
    }

    @Test
    void generateAttachment_shouldDoNothing_whenGraphIsUpToDate() {
        System system = mock(System.class);
        when(system.getName()).thenReturn("UpToDateSystem");

        SystemGraph graph = mock(SystemGraph.class);
        when(graph.getFingerprint()).thenReturn("abc123");
        when(graph.getLastPublishedFingerprint()).thenReturn("abc123");

        SystemGraphRepository graphRepo = mock(SystemGraphRepository.class);
        when(graphRepo.findBySystemNameIgnoreCase("UpToDateSystem")).thenReturn(graph);

        GraphvizRenderer renderer = mock(GraphvizRenderer.class);
        ConfluenceAdapter confluenceAdapter = mock(ConfluenceAdapter.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);

        SystemGraphAttachmentService service = new SystemGraphAttachmentService(
                graphRepo, renderer, confluenceAdapter, objectMapper
        );

        service.generateAttachment(system, "pageId");

        verifyNoInteractions(renderer);
        verifyNoInteractions(confluenceAdapter);
    }

    @Test
    void generateAttachment_shouldThrowRuntimeException_whenJsonParsingFails() throws Exception {
        System system = mock(System.class);
        when(system.getName()).thenReturn("BrokenSystem");

        SystemGraph graph = mock(SystemGraph.class);
        when(graph.getFingerprint()).thenReturn("new");
        when(graph.getLastPublishedFingerprint()).thenReturn("old");
        when(graph.getGraphData()).thenReturn("invalid-json".getBytes());

        SystemGraphRepository graphRepo = mock(SystemGraphRepository.class);
        when(graphRepo.findBySystemNameIgnoreCase("BrokenSystem")).thenReturn(graph);

        GraphvizRenderer renderer = mock(GraphvizRenderer.class);
        ConfluenceAdapter confluenceAdapter = mock(ConfluenceAdapter.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(any(byte[].class), eq(GraphDto.class)))
                .thenThrow(new IOException("Parsing failed"));

        SystemGraphAttachmentService service = new SystemGraphAttachmentService(
                graphRepo, renderer, confluenceAdapter, objectMapper
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.generateAttachment(system, "pageId"));

        assertTrue(ex.getMessage().contains("Error generating graph attachment"));
    }

    @Test
    void highlightOtherSystemsMessageNodes_shouldHighlightNodesFromOtherSystems() {
        System system = mock(System.class);
        when(system.getName()).thenReturn("MySystem");

        MessageNodeDto node1 = mock(MessageNodeDto.class);
        when(node1.getMessageType()).thenReturn("MySystem.Command");
        MessageNodeDto node2 = mock(MessageNodeDto.class);
        when(node2.getMessageType()).thenReturn("OtherSystem.Event");

        GraphDto graphDto = mock(GraphDto.class);
        when(graphDto.getNodes()).thenReturn(List.of(node1, node2));

        SystemGraphAttachmentService service = new SystemGraphAttachmentService(
                null, null, null, null
        );

        service.highlightOtherSystemsMessageNodes(graphDto, system);

        verify(node1, never()).setHighlighted(true);
        verify(node2).setHighlighted(true);
    }
}