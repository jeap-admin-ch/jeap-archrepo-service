package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.OpenApiSpecRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentDatabaseSchemaRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/**
 * The content resources of the docs API: the OpenAPI spec and the database schema of one component.
 * <p>
 * These do not load the architecture model - they go to the repositories directly, so that serving one artifact
 * does not assemble the whole landscape. They are pure reads and carry no write at all, so they can be routed to
 * a read replica. The content hash every entity tag is built from is written when an artifact is stored, and for
 * artifacts that predate the column it is filled by the {@code V2_6_0} migration - so it always exists here.
 */
@RestController
@RequestMapping(value = DocsApiPaths.SYSTEMS + "/{system}/components/{component}",
        produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
// Overrides the global list, which also offers basic auth: this API answers 401 to basic credentials
@SecurityRequirement(name = "OIDC")
@Tag(name = "docs-api-artifacts", description = "The OpenAPI spec and database schema of a component")
@TransactionalReadReplica
class ComponentArtifactController {

    private final SystemRepository systemRepository;
    private final SystemComponentRepository systemComponentRepository;
    private final OpenApiSpecRepository openApiSpecRepository;
    private final SystemComponentDatabaseSchemaRepository databaseSchemaRepository;
    private final DocsApiEtagSupport etagSupport;

    @GetMapping("/openapi")
    @RequiresArchitectureModelRead
    @Operation(summary = "The OpenAPI spec of a component",
            description = "The stored spec itself, byte for byte. Always application/json: the push path parses a "
                          + "spec as JSON before storing it, so a stored spec is always JSON.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The spec"),
            @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content),
            @ApiResponse(responseCode = "404", description = "Unknown system or component, or no spec published")})
    public ResponseEntity<byte[]> getOpenApiSpec(@PathVariable("system") String systemName,
                                                 @PathVariable("component") String componentName,
                                                 WebRequest request) {
        SystemComponent component = findComponent(systemName, componentName);
        OpenApiSpec spec = openApiSpecRepository.findByProvider(component)
                // A row with no content cannot be served and has no entity tag; it is an absent spec, not a 500
                .filter(stored -> stored.getContent() != null)
                .orElseThrow(() -> DocsApiException.openApiSpecNotFound(componentName));

        String entityTag = etagSupport.entityTag(spec.getContentHash());
        if (etagSupport.isNotModified(request, entityTag)) {
            return null;
        }
        return ResponseEntity.ok()
                .eTag(entityTag)
                .cacheControl(CacheControl.noCache())
                // The stored bytes, not a decoded copy of them: the entity tag names these bytes, and a UTF-8
                // round trip would replace anything malformed and put different bytes on the wire
                .body(spec.getContent());
    }

    @GetMapping("/database-schema")
    @RequiresArchitectureModelRead
    @Operation(summary = "The database schema of a component",
            description = "The stored schema itself, byte for byte: structured JSON with the tables, their "
                          + "columns, primary key and foreign keys, which is what an ERD is generated from.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The schema"),
            @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content),
            @ApiResponse(responseCode = "404", description = "Unknown system or component, or no schema published")})
    public ResponseEntity<byte[]> getDatabaseSchema(@PathVariable("system") String systemName,
                                                    @PathVariable("component") String componentName,
                                                    WebRequest request) {
        SystemComponent component = findComponent(systemName, componentName);
        SystemComponentDatabaseSchema schema = databaseSchemaRepository.findBySystemComponent(component)
                .orElseThrow(() -> DocsApiException.databaseSchemaNotFound(componentName));

        String entityTag = etagSupport.entityTag(schema.getContentHash());
        if (etagSupport.isNotModified(request, entityTag)) {
            return null;
        }
        return ResponseEntity.ok()
                .eTag(entityTag)
                .cacheControl(CacheControl.noCache())
                .body(schema.getSchema());
    }


    /**
     * Resolves the component and checks that it really belongs to the system in the path, so that the two path
     * segments cannot disagree. The system is matched by name or alias, as everywhere in this API.
     */
    private SystemComponent findComponent(String systemName, String componentName) {
        System system = systemRepository.findByNameOrAliasIgnoreCase(systemName)
                .orElseThrow(() -> DocsApiException.systemNotFound(systemName));
        return systemComponentRepository.findByNameIgnoreCase(componentName)
                .filter(component -> component.getParent() != null
                                     && component.getParent().getId().equals(system.getId()))
                .orElseThrow(() -> DocsApiException.componentNotFound(systemName, componentName));
    }
}
