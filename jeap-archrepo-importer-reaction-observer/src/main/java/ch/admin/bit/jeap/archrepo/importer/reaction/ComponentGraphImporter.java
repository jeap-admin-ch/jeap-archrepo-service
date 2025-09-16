package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.importer.reaction.client.GraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.system.ComponentGraph;
import ch.admin.bit.jeap.archrepo.persistence.ComponentGraphRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
class ComponentGraphImporter implements ArchRepoImporter {

    private final ReactionObserverService reactionObserverService;
    private final ComponentGraphRepository componentGraphRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void importIntoModel(ArchitectureModel model, String environment) {
        log.info("Getting service graphs from Reaction Observer Service...");
        Map<String, String> componentToSystemMap = model.getAllSystemComponentNamesWithSystemName();

        for (Map.Entry<String, String> entry : componentToSystemMap.entrySet()) {
            importComponent(entry.getKey(), entry.getValue());
        }
        log.info("Service graph import completed");
    }

    private void importComponent(String componentName, String systemName) {
        log.trace("Processing component: {} from system: {}", componentName, systemName);
        try {
            GraphDto graphDto = reactionObserverService.getComponentGraph(componentName);
            if (graphDto == null) {
                log.warn("No graph data found for component: {} in system: {}", componentName, systemName);
                return;
            }
            saveOrUpdateComponentGraph(componentName, systemName, graphDto);
        } catch (Exception e) {
            log.warn("Failed to get or process graph data for component: {} in system: {}", componentName, systemName, e);
        }
    }

    private void saveOrUpdateComponentGraph(String componentName, String systemName, GraphDto graphDto) throws Exception {
        byte[] graphData = objectMapper.writeValueAsBytes(graphDto.graph());
        if (componentGraphRepository.existsBySystemNameAndComponentName(systemName, componentName)) {
            componentGraphRepository.updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(
                    systemName, componentName, graphData, graphDto.fingerprint());
            log.trace("Updated graph for component: {} in system: {} with fingerprint: {}",
                    componentName, systemName, graphDto.fingerprint());
        } else {
            ComponentGraph componentGraph = ComponentGraph.builder()
                    .systemName(systemName)
                    .componentName(componentName)
                    .graphData(graphData)
                    .fingerprint(graphDto.fingerprint())
                    .build();
            componentGraphRepository.save(componentGraph);
            log.trace("Saved new graph for component: {} in system: {} with fingerprint: {}",
                    componentName, systemName, graphDto.fingerprint());
        }
    }
}
