package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.persistence.ArchitectureModelRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/**
 * The model resources of the docs API: the system list, the per-system export and the message export.
 */
@RestController
@RequestMapping(value = DocsApiPaths.SYSTEMS, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
// Overrides the global list, which also offers basic auth: this API answers 401 to basic credentials
@SecurityRequirement(name = "OIDC")
@Tag(name = "docs-api-systems", description = "The architecture model, for the jEAP Doc Service")
@TransactionalReadReplica
class SystemsController {

    private final ArchitectureModelRepository architectureModelRepository;
    private final DocsApiDtoFactory dtoFactory;
    private final DocsApiEtagSupport etagSupport;

    @GetMapping
    @RequiresArchitectureModelRead
    @Operation(summary = "Every system with its team and aliases",
            description = "The index a documentation generation run iterates over. Deliberately light: no "
                          + "components and no relations.")
    @ApiResponse(responseCode = "200", description = "The systems",
                    content = @Content(schema = @Schema(implementation = SystemListDto.class)))
    @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content)
    @ApiResponse(responseCode = "401", description = "No or invalid token", content = @Content)
    @ApiResponse(responseCode = "403", description = "The token lacks the architecture-model read role", content = @Content)
    public ResponseEntity<byte[]> getSystems(WebRequest request) {
        SystemListDto body = dtoFactory.createSystemList(architectureModelRepository.load());
        return etagSupport.respond(request, body);
    }

    @GetMapping("/{system}")
    @RequiresArchitectureModelRead
    @Operation(summary = "One system with its components and relations",
            description = "Everything the system page and its component pages need, in one response. The system "
                          + "is matched by name or by alias, ignoring case.")
    @ApiResponse(responseCode = "200", description = "The system",
                    content = @Content(schema = @Schema(implementation = SystemDetailDto.class)))
    @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content)
    @ApiResponse(responseCode = "404", description = "No system of that name or alias")
    public ResponseEntity<byte[]> getSystem(@PathVariable("system") String systemName, WebRequest request) {
        ArchitectureModel model = architectureModelRepository.load();
        System system = findSystem(model, systemName);
        return etagSupport.respond(request, dtoFactory.createSystemDetail(model, system));
    }

    @GetMapping("/{system}/messages")
    @RequiresArchitectureModelRead
    @Operation(summary = "The events and commands defined by a system",
            description = "Kept apart from the system export because message sets are large and change on a "
                          + "different rhythm than the component topology.")
    @ApiResponse(responseCode = "200", description = "The messages",
                    content = @Content(schema = @Schema(implementation = MessageListDto.class)))
    @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content)
    @ApiResponse(responseCode = "404", description = "No system of that name or alias")
    public ResponseEntity<byte[]> getMessages(@PathVariable("system") String systemName, WebRequest request) {
        ArchitectureModel model = architectureModelRepository.load();
        System system = findSystem(model, systemName);
        return etagSupport.respond(request, dtoFactory.createMessageList(model, system));
    }

    private System findSystem(ArchitectureModel model, String systemName) {
        return model.getSystems().stream()
                .filter(system -> matchesNameOrAlias(system, systemName))
                .findFirst()
                .orElseThrow(() -> DocsApiException.systemNotFound(systemName));
    }

    private boolean matchesNameOrAlias(System system, String systemName) {
        return system.getName().equalsIgnoreCase(systemName)
               || system.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(systemName));
    }
}
