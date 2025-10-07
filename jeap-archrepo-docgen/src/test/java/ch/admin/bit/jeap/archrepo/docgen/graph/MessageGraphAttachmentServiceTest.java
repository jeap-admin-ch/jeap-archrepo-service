package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.ConfluenceAdapter;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.persistence.MessageGraphRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MessageGraphAttachmentServiceTest {

    private MessageGraphRepository repository;
    private GraphvizRenderer renderer;
    private ConfluenceAdapter adapter;
    private ObjectMapper objectMapper;
    private MessageGraphAttachmentService service;

    @BeforeEach
    void setup() {
        repository = mock(MessageGraphRepository.class);
        renderer = mock(GraphvizRenderer.class);
        adapter = mock(ConfluenceAdapter.class);
        objectMapper = mock(ObjectMapper.class);
        service = new MessageGraphAttachmentService(repository, renderer, adapter, objectMapper);
    }

    private MessageGraph createGraph(String type, String variant, String fingerprint, String actualFingerprint, byte[] graphData) {
        return MessageGraph.builder()
                .messageTypeName(type)
                .variant(variant)
                .fingerprint(fingerprint)
                .actualDocFingerprint(actualFingerprint)
                .graphData(graphData)
                .build();
    }

    private MessageType mockMessageType(String name) {
        MessageType mock = mock(MessageType.class);
        when(mock.getMessageTypeName()).thenReturn(name);
        return mock;
    }

    @Test
    void testGetAttachmentNames_withMockedCommand() {
        MessageType message = mockMessageType("MyCommand");
        MessageGraph g1 = createGraph("MyCommand", "", "fp", "fp", "{}".getBytes());
        MessageGraph g2 = createGraph("MyCommand", "v1", "fp", "fp", "{}".getBytes());

        when(repository.findAllByMessageTypeName("MyCommand")).thenReturn(List.of(g1, g2));

        List<String> names = service.getAttachmentNames(message);

        assertEquals(List.of("graph-MyCommand.png", "graph-MyCommand-v1.png"), names);
    }

    @Test
    void testFilterOutdatedGraphs_filtersCorrectly() {
        MessageGraph g1 = createGraph("msg", "", "fp1", "fp1", "{}".getBytes());
        MessageGraph g2 = createGraph("msg", "", "fp2", "fpX", "{}".getBytes());

        List<MessageGraph> result = service.filterOutdatedGraphs(List.of(g1, g2));

        assertEquals(1, result.size());
        assertEquals(g2, result.getFirst());
    }

    @Test
    void testHighlightMessageNode_setsHighlightCorrectly() {
        MessageType message = mockMessageType("MyEvent");
        MessageNodeDto node = new MessageNodeDto();
        node.setMessageType("MyEvent");
        node.setHighlighted(false);

        GraphDto graph = new GraphDto();
        graph.setNodes(List.of(node));

        service.highlightMessageNode(graph, message);

        assertTrue(node.isHighlighted());
    }

    @Test
    void testGenerateMessageAttachmentName_withVariant() {
        MessageGraph graph = createGraph("MyMessage", "v1", "fp", "fp", "{}".getBytes());
        String name = service.generateMessageAttachmentName(graph);
        assertEquals("graph-MyMessage-v1.png", name);
    }

    @Test
    void testGenerateMessageAttachmentName_withoutVariant() {
        MessageGraph graph = createGraph("MyMessage", "", "fp", "fp", "{}".getBytes());
        String name = service.generateMessageAttachmentName(graph);
        assertEquals("graph-MyMessage.png", name);
    }

    @Test
    void testGenerateAttachments_updatesOutdatedGraphs() throws Exception {
        MessageType message = mockMessageType("MyCommand");
        byte[] graphData = "{}".getBytes();
        MessageGraph outdatedGraph = createGraph("MyCommand", "", "fp-new", "fp-old", graphData);

        GraphDto graphDto = new GraphDto();
        graphDto.setNodes(List.of());

        when(repository.findAllByMessageTypeName("MyCommand")).thenReturn(List.of(outdatedGraph));
        when(objectMapper.readValue(eq(graphData), eq(GraphDto.class))).thenReturn(graphDto);
        when(renderer.renderPng(any())).thenReturn(new ByteArrayInputStream(new byte[]{1}));

        service.generateAttachments(message, "pageId");

        verify(adapter).addOrUpdateAttachment(eq("pageId"), eq("graph-MyCommand.png"), any(InputStream.class));
        verify(repository).updateActualDocFingerprint(eq(outdatedGraph.getId()), eq("fp-new"));
    }

    @Test
    void testGenerateAttachments_handlesEmptyGraphList() {
        MessageType message = mockMessageType("EmptyEvent");
        when(repository.findAllByMessageTypeName("EmptyEvent")).thenReturn(List.of());

        service.generateAttachments(message, "pageId");

        verify(adapter, never()).addOrUpdateAttachment(any(), any(), any());
        verify(repository, never()).updateActualDocFingerprint(any(), any());
    }

    @Test
    void testGenerateAttachments_throwsRuntimeExceptionOnIOException() throws Exception {
        MessageType message = mockMessageType("MyCommand");
        byte[] graphData = "{}".getBytes();
        MessageGraph graph = createGraph("MyCommand", "", "fp-new", "fp-old", graphData);

        when(repository.findAllByMessageTypeName("MyCommand")).thenReturn(List.of(graph));
        when(objectMapper.readValue(eq(graphData), eq(GraphDto.class))).thenThrow(new IOException("Parsing failed"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.generateAttachments(message, "pageId");
        });

        assertTrue(ex.getMessage().contains("Error generating graph attachments"));
    }

    @Test
    void testDeleteUnusedGraphAttachments_calledWithCorrectNames() throws Exception {
        MessageType message = mockMessageType("MyEvent");
        MessageGraph g1 = createGraph("MyEvent", "", "fp", "fp", "{}".getBytes());
        MessageGraph g2 = createGraph("MyEvent", "v1", "fp", "fp", "{}".getBytes());

        when(repository.findAllByMessageTypeName("MyEvent")).thenReturn(List.of(g1, g2));
        when(objectMapper.readValue(any(byte[].class), eq(GraphDto.class))).thenReturn(new GraphDto());
        when(renderer.renderPng(any())).thenReturn(new ByteArrayInputStream(new byte[]{1}));

        service.generateAttachments(message, "pageId");

        verify(adapter).deleteUnusedAttachments(eq("pageId"), eq(List.of("graph-MyEvent.png", "graph-MyEvent-v1.png")));
    }
}
