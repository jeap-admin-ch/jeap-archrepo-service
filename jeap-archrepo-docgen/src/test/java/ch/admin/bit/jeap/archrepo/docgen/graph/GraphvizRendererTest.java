package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphvizRendererTest {

    @Test
    void renderPng_shouldReturnImageStream_whenGraphvizSucceeds() throws Exception {
        GraphDto graphDto = mock(GraphDto.class);
        when(graphDto.toDot()).thenReturn("digraph G { A -> B }");

        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(mockProcess.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(mockProcess.waitFor()).thenReturn(0);

        GraphvizRenderer renderer = spy(new GraphvizRenderer());
        doReturn(mockProcess).when(renderer).startGraphvizProcess();

        InputStream result = renderer.renderPng(graphDto);

        assertNotNull(result);
        byte[] bytes = result.readAllBytes();
        assertArrayEquals(new byte[]{1, 2, 3}, bytes);
    }


    @Test
    void renderPng_shouldThrowException_whenWritingFails() throws Exception {
        GraphDto graphDto = mock(GraphDto.class);
        when(graphDto.toDot()).thenReturn("digraph G { A -> B }");

        Process mockProcess = mock(Process.class);
        OutputStream failingStream = mock(OutputStream.class);
        when(mockProcess.getOutputStream()).thenReturn(failingStream);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(mockProcess.waitFor()).thenReturn(0);

        doThrow(new RuntimeException("Write failed")).when(failingStream).close();

        GraphvizRenderer renderer = spy(new GraphvizRenderer());
        doReturn(mockProcess).when(renderer).startGraphvizProcess();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> renderer.renderPng(graphDto));
        assertTrue(ex.getMessage().contains("Error while rendering the graph."));
    }
}