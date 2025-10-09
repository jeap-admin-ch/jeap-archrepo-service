package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GraphvizRenderer {

    public InputStream renderPng(GraphDto graph) {
        String dot = graph.toDot();
        log.debug("DOT content generated:\n{}", dot);

        try {
            Process process = startGraphvizProcess();
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
        }
    }

    Process startGraphvizProcess() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng");
        log.debug("Starting Graphviz process");
        return pb.start();
    }

    private void writeDotToProcess(String dot, Process process) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(dot);
        }
    }

    private byte[] readProcessOutput(Process process) throws IOException {
        try (InputStream processOut = process.getInputStream();
             ByteArrayOutputStream pngOutput = new ByteArrayOutputStream()) {
            processOut.transferTo(pngOutput);
            return pngOutput.toByteArray();
        }
    }

    private void validateProcessExit(Process process) throws InterruptedException, IOException {
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
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