package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

@Slf4j
@Component
public class GraphvizRenderer {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final long timeoutSeconds;

    public GraphvizRenderer() {
        this.timeoutSeconds = 60;
    }

    public GraphvizRenderer(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdownNow();
    }

    public InputStream renderPng(GraphDto graph) {
        String dot = graph.toDot();
        log.debug("DOT content generated:\n{}", dot);

        Process process = null;
        try {
            process = startGraphvizProcess();

            try (OutputStream processInput = process.getOutputStream();
                 InputStream processOutput = process.getInputStream()) {

                writeDotToProcess(dot, processInput);
                byte[] imageBytes = readProcessOutput(processOutput);

                log.debug("Graph image rendering completed successfully");
                return new ByteArrayInputStream(imageBytes);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Graph rendering was interrupted.", e);
            throw new RuntimeException("Graph rendering was interrupted.", e);
        } catch (Exception e) {
            log.error("Error while rendering the graph.", e);
            throw new RuntimeException("Error while rendering the graph.", e);
        } finally {
            if (process != null) {
                validateProcessExit(process);
            }
        }
    }

    private void writeDotToProcess(String dot, OutputStream outputStream) throws Exception {
        Future<?> writeFuture = executor.submit(() -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                writer.write(dot);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        try {
            writeFuture.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            writeFuture.cancel(true);
            throw new RuntimeException("Timeout while writing DOT to Graphviz process", e);
        }
    }

    private byte[] readProcessOutput(InputStream inputStream) throws Exception {
        Future<byte[]> readFuture = executor.submit(() -> {
            try (ByteArrayOutputStream pngOutput = new ByteArrayOutputStream()) {
                inputStream.transferTo(pngOutput);
                return pngOutput.toByteArray();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        try {
            return readFuture.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            readFuture.cancel(true);
            throw new RuntimeException("Timeout while reading output from Graphviz process", e);
        }
    }

    Process startGraphvizProcess() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng");
        log.debug("Starting Graphviz process");
        return pb.start();
    }

    void validateProcessExit(Process process) {
        try {
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("Graphviz process timed out and was forcibly terminated.");
                return;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Graphviz failed with exit code {}", exitCode);
            }
        } catch (Exception e) {
            log.error("Unexpected error during Graphviz process validation.", e);
        }
    }
}