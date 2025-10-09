package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.ConfluenceAdapter;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.metamodel.system.ComponentGraph;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.ComponentGraphRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComponentGraphAttachmentServiceTest {

    @Test
    void generateAttachment_shouldRenderAndUploadGraph_whenGraphIsOutdated() throws Exception {
        String componentName = "TestComponent";
        String pageId = "12345";
        String fingerprint = "new-fingerprint";
        String oldFingerprint = "old-fingerprint";

        SystemComponent component = mock(SystemComponent.class);
        when(component.getName()).thenReturn(componentName);

        ComponentGraph graph = mock(ComponentGraph.class);
        when(graph.getFingerprint()).thenReturn(fingerprint);
        when(graph.getLastPublishedFingerprint()).thenReturn(oldFingerprint);
        when(graph.getGraphData()).thenReturn("{\"nodes\":[],\"edges\":[]}".getBytes());

        ComponentGraphRepository graphRepo = mock(ComponentGraphRepository.class);
        when(graphRepo.findByComponentNameIgnoreCase(componentName)).thenReturn(graph);

        GraphDto graphDto = mock(GraphDto.class);
        when(graphDto.toDot()).thenReturn("digraph G { A -> B }");

        GraphvizRenderer renderer = mock(GraphvizRenderer.class);
        InputStream dummyImageStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(renderer.renderPng(any())).thenReturn(dummyImageStream);

        ConfluenceAdapter confluenceAdapter = mock(ConfluenceAdapter.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(any(byte[].class), eq(GraphDto.class))).thenReturn(graphDto);

        ComponentGraphAttachmentService service = new ComponentGraphAttachmentService(
                graphRepo, renderer, confluenceAdapter, objectMapper
        );

        service.generateAttachment(component, pageId);

        verify(renderer).renderPng(graphDto);
        verify(confluenceAdapter).addOrUpdateAttachment(eq(pageId), eq("graph-TestComponent.png"), eq(dummyImageStream));
        verify(graphRepo).updateLastPublishedFingerprint(graph.getId(), fingerprint);
    }


    @Test
    void generateAttachment_shouldDoNothing_whenNoGraphExists() {
        SystemComponent component = mock(SystemComponent.class);
        when(component.getName()).thenReturn("MissingComponent");

        ComponentGraphRepository graphRepo = mock(ComponentGraphRepository.class);
        when(graphRepo.findByComponentNameIgnoreCase("MissingComponent")).thenReturn(null);

        GraphvizRenderer renderer = mock(GraphvizRenderer.class);
        ConfluenceAdapter confluenceAdapter = mock(ConfluenceAdapter.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);

        ComponentGraphAttachmentService service = new ComponentGraphAttachmentService(
                graphRepo, renderer, confluenceAdapter, objectMapper
        );

        service.generateAttachment(component, "pageId");

        verifyNoInteractions(renderer);
        verifyNoInteractions(confluenceAdapter);
    }

    @Test
    void generateAttachment_shouldDoNothing_whenGraphIsUpToDate() {
        SystemComponent component = mock(SystemComponent.class);
        when(component.getName()).thenReturn("UpToDateComponent");

        ComponentGraph graph = mock(ComponentGraph.class);
        when(graph.getFingerprint()).thenReturn("abc123");
        when(graph.getLastPublishedFingerprint()).thenReturn("abc123");

        ComponentGraphRepository graphRepo = mock(ComponentGraphRepository.class);
        when(graphRepo.findByComponentNameIgnoreCase("UpToDateComponent")).thenReturn(graph);

        GraphvizRenderer renderer = mock(GraphvizRenderer.class);
        ConfluenceAdapter confluenceAdapter = mock(ConfluenceAdapter.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);

        ComponentGraphAttachmentService service = new ComponentGraphAttachmentService(
                graphRepo, renderer, confluenceAdapter, objectMapper
        );

        service.generateAttachment(component, "pageId");

        verifyNoInteractions(renderer);
        verifyNoInteractions(confluenceAdapter);
    }

    @Test
    void generateAttachment_shouldThrowRuntimeException_whenJsonParsingFails() throws Exception {
        SystemComponent component = mock(SystemComponent.class);
        when(component.getName()).thenReturn("BrokenComponent");

        ComponentGraph graph = mock(ComponentGraph.class);
        when(graph.getFingerprint()).thenReturn("new");
        when(graph.getLastPublishedFingerprint()).thenReturn("old");
        when(graph.getGraphData()).thenReturn("invalid-json".getBytes());

        ComponentGraphRepository graphRepo = mock(ComponentGraphRepository.class);
        when(graphRepo.findByComponentNameIgnoreCase("BrokenComponent")).thenReturn(graph);

        GraphvizRenderer renderer = mock(GraphvizRenderer.class);
        ConfluenceAdapter confluenceAdapter = mock(ConfluenceAdapter.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(any(byte[].class), eq(GraphDto.class)))
                .thenThrow(new IOException("Parsing failed"));

        ComponentGraphAttachmentService service = new ComponentGraphAttachmentService(
                graphRepo, renderer, confluenceAdapter, objectMapper
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.generateAttachment(component, "pageId"));

        assertTrue(ex.getMessage().contains("Error generating graph attachment"));
    }

}
