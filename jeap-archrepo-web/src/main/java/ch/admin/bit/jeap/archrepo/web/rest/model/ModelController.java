package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.persistence.ArchitectureModelRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
@Tag(name = "model", description = "Architecture meta model")
@Slf4j
@Transactional(readOnly = true)
class ModelController {

    private final ArchitectureModelRepository repository;

    private final ModelDtoFactory dtoFactory;
    private final ModelDtoFactory modelDtoFactory;

    @GetMapping
    @Operation(
            summary = "Structural architecture model without relations",
            description = "Get a list of all systems and components")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ModelDto.class),
                            examples = @ExampleObject(name = "relations", ref = "#/components/examples/modelExample")))
    })
    public ModelDto getModel() {
        ArchitectureModel model = repository.load();
        return dtoFactory.createModelDto(model);
    }

    @GetMapping("/rest-api-relation-without-pact")
    @Operation(
            summary = "Represents REST relations with missing Pact tests",
            description = "Get all REST API relations for which no Pact test was found")
    public List<RestApiRelationDto> getAllRestApiRelationsWithoutPact() {
        final ArchitectureModel model = repository.load();
        final Map<String, String> servicesAndSystems = model.getAllSystemComponentNamesWithSystemName();
        return model.getRestApiRelationsWithoutPact().stream()
                .map(relation -> modelDtoFactory.createRestApiRelationDto(servicesAndSystems, relation))
                .toList();
    }

    @GetMapping("/system-components-without-open-api-spec")
    @Operation(
            summary = "Represents System Components without open api spec",
            description = "Get all System Components for which open api spec was defined")
    public List<String> getSystemComponentsWithoutOpenApiSpec() {
        final ArchitectureModel model = repository.load();
        return model.getSystemComponentsWithoutOpenApiSpec();
    }

    @GetMapping("/{system}/relations")
    @Operation(
            summary = "All relations (REST, messaging, ...)",
            description = "Get all relations (REST, messaging, ...) for a certain system. Use /api/model to discover available systems and components.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful response",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = RelationDto.class)),
                            examples = @ExampleObject(name = "relations", ref = "#/components/examples/relationsExample"))),
            @ApiResponse(responseCode = "404", description = "No system with this name exists", content = @Content(mediaType = "application/json"))
    })
    public List<RelationDto> getAllRelations(@PathVariable("system") String systemName) {
        final ArchitectureModel model = repository.load();
        System system = model.getSystems().stream()
                .filter(s -> s.getName().equalsIgnoreCase(systemName))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "System not found"));
        return dtoFactory.createRelationDtos(model, system);
    }
}
