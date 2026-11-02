package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.persistence.ArchitectureModelRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/model/export")
@RequiredArgsConstructor
public class ModelExportController {
    private final ArchitectureModelRepository repository;
    private final YAMLMapper yamlMapper = YAMLMapper.builder().build();

    // Not readOnly/TransactionalReadReplica: bootstrap must read the primary.
    @Transactional
    @GetMapping(produces = "application/yaml")
    @Operation(summary = "Export all systems and teams as deterministic archrepo.model configuration")
    public String export() {
        return yamlMapper.writeValueAsString(document(repository.load()));
    }

    private static Map<String, Object> document(ArchitectureModel model) {
        Map<String, Object> definitions = new LinkedHashMap<>();
        definitions.put("teams", model.getTeams().stream().sorted(Comparator.comparing(Team::getName))
                .map(ModelExportController::team).toList());
        definitions.put("systems", model.getSystems().stream().sorted(Comparator.comparing(System::getName))
                .map(ModelExportController::system).toList());
        return Map.of("archrepo", Map.of("model", definitions));
    }

    private static Map<String, Object> team(Team team) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("name", team.getName());
        put(fields, "contact-address", team.getContactAddress());
        put(fields, "confluence-link", team.getConfluenceLink());
        put(fields, "jira-link", team.getJiraLink());
        return fields;
    }

    private static Map<String, Object> system(System system) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("name", system.getName());
        fields.put("team", system.getDefaultOwner().getName());
        put(fields, "description", system.getDescription());
        put(fields, "confluence-link", system.getConfluenceLink());
        fields.put("aliases", system.getAliases().stream().sorted().toList());
        return fields;
    }

    private static void put(Map<String, Object> fields, String name, String value) {
        if (value != null) {
            fields.put(name, value);
        }
    }
}
