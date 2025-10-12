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
            writeDotToProcess(dot, process);
            byte[] imageBytes = readProcessOutput(process);
            validateProcessExit(process);

            log.debug("Graph image rendering completed successfully");
            return new ByteArrayInputStream(imageBytes);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Graph rendering was interrupted.", e);
            throw new RuntimeException("Graph rendering was interrupted.", e);
        } catch (Exception e) {
            log.error("Error while rendering the graph.", e);
            throw new RuntimeException("Error while rendering the graph.", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    Process startGraphvizProcess() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng");
        log.debug("Starting Graphviz process");
        return pb.start();
    }

    private void writeDotToProcess(String dot, Process process) throws Exception {
        Future<?> writeFuture = executor.submit(() -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
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

    private byte[] readProcessOutput(Process process) throws Exception {
        Future<byte[]> readFuture = executor.submit(() -> {
            try (InputStream processOut = process.getInputStream();
                 ByteArrayOutputStream pngOutput = new ByteArrayOutputStream()) {
                processOut.transferTo(pngOutput);
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

    private void validateProcessExit(Process process) throws InterruptedException, IOException {
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Graphviz process timed out and was forcibly terminated.");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errorOutput = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("Graphviz failed with exit code " + exitCode + ". Error output:\n" + errorOutput);
        }
    }
}