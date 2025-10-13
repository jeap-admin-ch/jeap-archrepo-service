package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.concurrent.TimeUnit;

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
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
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
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        GraphvizRenderer renderer = spy(new GraphvizRenderer());
        doReturn(mockProcess).when(renderer).startGraphvizProcess();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> renderer.renderPng(graphDto));
        assertTrue(ex.getMessage().contains("Error while rendering the graph."));
    }

    @Test
    void validateProcessExit_shouldDestroyProcess_whenProcessTimesOut() {
        Process mockProcess = mock(Process.class);
        try {
            when(mockProcess.waitFor(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(false);
            when(mockProcess.exitValue()).thenReturn(0);
        } catch (InterruptedException e) {
            fail("Unexpected InterruptedException");
        }

        GraphvizRenderer renderer = new GraphvizRenderer();
        renderer.validateProcessExit(mockProcess);

        verify(mockProcess).destroyForcibly();
    }

    @Test
    void renderPng_shouldDestroyProcess_whenValidationFails() throws Exception {
        GraphDto graphDto = mock(GraphDto.class);
        when(graphDto.toDot()).thenReturn("digraph G { A -> B }");

        Process mockProcess = mock(Process.class);
        when(mockProcess.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(false); // simulate timeout
        when(mockProcess.isAlive()).thenReturn(true);

        GraphvizRenderer renderer = spy(new GraphvizRenderer());
        doReturn(mockProcess).when(renderer).startGraphvizProcess();

        renderer.renderPng(graphDto);

        verify(mockProcess).destroyForcibly();
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
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        GraphvizRenderer renderer = spy(new GraphvizRenderer(1));
        doReturn(mockProcess).when(renderer).startGraphvizProcess();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> renderer.renderPng(graphDto));
        assertTrue(ex.getMessage().contains("Error while rendering the graph."));
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
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[]{}));
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        GraphvizRenderer renderer = spy(new GraphvizRenderer(1));
        doReturn(mockProcess).when(renderer).startGraphvizProcess();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> renderer.renderPng(graphDto));
        assertTrue(ex.getMessage().contains("Error while rendering the graph."));
    }
}