package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.importer.reaction.client.GraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemGraph;
import ch.admin.bit.jeap.archrepo.persistence.SystemGraphRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
class SystemGraphImporter implements ArchRepoImporter {

    private final ReactionObserverService reactionObserverService;
    private final SystemGraphRepository systemGraphRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void importIntoModel(ArchitectureModel model, String environment) {
        log.info("Getting system graphs from Reaction Observer Service...");
        for (String systemName : reactionObserverService.getSystemNames()) {
            importSystem(systemName);
        }
        log.info("System graph import completed");
    }

    private void importSystem(String systemName) {
        log.trace("Processing system: {}", systemName);
        try {
            GraphDto graphDto = reactionObserverService.getSystemGraph(systemName);
            if (graphDto == null) {
                log.warn("No graph data found for system: {}", systemName);
                return;
            }
            saveOrUpdateSystemGraph(systemName, graphDto);
        } catch (Exception e) {
            log.warn("Failed to get or process graph data for system: {}", systemName, e);
        }
    }

    private void saveOrUpdateSystemGraph(String systemName, GraphDto graphDto) throws Exception {
        byte[] graphData = objectMapper.writeValueAsBytes(graphDto.graph());
        if (systemGraphRepository.existsBySystemName(systemName)) {
            systemGraphRepository.updateGraphDataAndFingerprintIfFingerprintChanged(
                    systemName, graphData, graphDto.fingerprint());
            log.trace("Updated graph for system: {} with fingerprint: {}",
                    systemName, graphDto.fingerprint());
        } else {
            SystemGraph systemGraph = SystemGraph.builder()
                    .systemName(systemName)
                    .graphData(graphData)
                    .fingerprint(graphDto.fingerprint())
                    .build();
            systemGraphRepository.save(systemGraph);
            log.trace("Saved new graph for system: {} with fingerprint: {}",
                    systemName, graphDto.fingerprint());
        }
    }
}
