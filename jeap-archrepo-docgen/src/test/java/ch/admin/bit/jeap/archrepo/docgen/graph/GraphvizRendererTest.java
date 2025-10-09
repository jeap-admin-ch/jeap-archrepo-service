package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;

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
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

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

        OutputStream failingStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("Write failed");
            }
        };

        Process mockProcess = mock(Process.class);
        when(mockProcess.getOutputStream()).thenReturn(failingStream);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        GraphvizRenderer renderer = spy(new GraphvizRenderer());
        doReturn(mockProcess).when(renderer).startGraphvizProcess();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> renderer.renderPng(graphDto));
        assertTrue(ex.getMessage().contains("Error while rendering the graph."));
    }

    @Test
    void renderPng_shouldThrowException_whenProcessTimesOut() throws Exception {
        GraphDto graphDto = mock(GraphDto.class);
        when(graphDto.toDot()).thenReturn("digraph G { A -> B }");

        Process mockProcess = mock(Process.class);
        when(mockProcess.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(false); // simulate timeout

        GraphvizRenderer renderer = spy(new GraphvizRenderer());
        doReturn(mockProcess).when(renderer).startGraphvizProcess();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> renderer.renderPng(graphDto));
        assertTrue(ex.getCause().getMessage().contains("Graphviz process timed out and was forcibly terminated."));
    }

    @Test
    void renderPng_shouldThrowException_whenProcessFailsWithErrorOutput() throws Exception {
        GraphDto graphDto = mock(GraphDto.class);
        when(graphDto.toDot()).thenReturn("digraph G { A -> B }");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{});
        ByteArrayInputStream errorStream = new ByteArrayInputStream("Error: invalid DOT".getBytes());

        Process mockProcess = mock(Process.class);
        when(mockProcess.getOutputStream()).thenReturn(outputStream);
        when(mockProcess.getInputStream()).thenReturn(inputStream);
        when(mockProcess.getErrorStream()).thenReturn(errorStream);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(1); // simulate error

        GraphvizRenderer renderer = spy(new GraphvizRenderer());
        doReturn(mockProcess).when(renderer).startGraphvizProcess();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> renderer.renderPng(graphDto));

        assertTrue(ex.getCause().getMessage().contains("Graphviz failed with exit code 1"));
        assertTrue(ex.getCause().getMessage().contains("Error: invalid DOT"));

    }

    @Test
    void renderPng_shouldThrowException_whenWritingTimesOut() throws Exception {
        GraphDto graphDto = mock(GraphDto.class);
        when(graphDto.toDot()).thenReturn("digraph G { A -> B }");

        OutputStream blockingStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    throw new IOException("Interrupted during write", e);
                }
            }
        };

        Process mockProcess = mock(Process.class);
        when(mockProcess.getOutputStream()).thenReturn(blockingStream);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        GraphvizRenderer renderer = spy(new GraphvizRenderer(1));
        doReturn(mockProcess).when(renderer).startGraphvizProcess();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> renderer.renderPng(graphDto));
        assertTrue(ex.getCause().getMessage().contains("Timeout while writing DOT to Graphviz process"));
    }

    @Test
    void renderPng_shouldThrowException_whenReadingTimesOut() throws Exception {
        GraphDto graphDto = mock(GraphDto.class);
        when(graphDto.toDot()).thenReturn("digraph G { A -> B }");

        InputStream blockingInput = new InputStream() {
            @Override
            public int read() throws IOException {
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    throw new IOException("Interrupted during read", e);
                }
                return -1;
            }
        };

        Process mockProcess = mock(Process.class);
        when(mockProcess.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(mockProcess.getInputStream()).thenReturn(blockingInput);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        GraphvizRenderer renderer = spy(new GraphvizRenderer(1));
        doReturn(mockProcess).when(renderer).startGraphvizProcess();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> renderer.renderPng(graphDto));
        assertTrue(ex.getCause().getMessage().contains("Timeout while reading output from Graphviz process"));
    }
}