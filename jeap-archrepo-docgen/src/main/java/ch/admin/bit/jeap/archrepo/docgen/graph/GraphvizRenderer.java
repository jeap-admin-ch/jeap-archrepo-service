package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;

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

    private void validateProcessExit(Process process) throws InterruptedException {
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Graphviz failed with exit code " + exitCode);
        }
    }
}