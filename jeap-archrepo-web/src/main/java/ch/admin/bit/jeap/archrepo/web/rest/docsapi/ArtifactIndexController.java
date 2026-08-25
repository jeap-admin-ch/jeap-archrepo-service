package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.db.tx.TransactionalReadReplica;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.persistence.ArtifactIndexEntry;
import ch.admin.bit.jeap.archrepo.persistence.OpenApiSpecRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentDatabaseSchemaRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The replication indexes: every available OpenAPI spec and database schema, each with the entity tag of its
 * content.
 * <p>
 * The point of an index is that a consumer learns in one call which artifacts it has to fetch. Its {@code etag}
 * is byte-identical to the {@code ETag} header of the content resource it points at, so the comparison needs no
 * request; and the index lists what is <em>available</em> rather than what changed, because otherwise
 * "unchanged" and "deleted" would be indistinguishable.
 */
@RestController
@RequiredArgsConstructor
// Overrides the global list, which also offers basic auth: this API answers 401 to basic credentials
@SecurityRequirement(name = "OIDC")
@Tag(name = "docs-api-indexes", description = "Which artifacts exist, and their entity tags")
@TransactionalReadReplica
class ArtifactIndexController {

    private final SystemRepository systemRepository;
    private final OpenApiSpecRepository openApiSpecRepository;
    private final SystemComponentDatabaseSchemaRepository databaseSchemaRepository;
    private final DocsApiEtagSupport etagSupport;

    @GetMapping(value = DocsApiPaths.OPENAPI_SPECS, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequiresArchitectureModelRead
    @Operation(summary = "Every published OpenAPI spec, with its entity tag",
            description = "One call tells a consumer which specs changed since it last replicated them.")
    @ApiResponse(responseCode = "200", description = "The index",
                    content = @Content(schema = @Schema(implementation = ArtifactIndexDto.class)))
    @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content)
    @ApiResponse(responseCode = "404", description = "The system filter names no known system or alias")
    public ResponseEntity<byte[]> getOpenApiSpecs(
            @Parameter(description = "Restrict the index to one system, by name or alias") @RequestParam(required = false) String system,
            WebRequest request) {
        return respond(request, index(system,
                openApiSpecRepository::findIndexEntries,
                openApiSpecRepository::findIndexEntriesBySystemName,
                DocsApiPaths::openApiContentPath));
    }

    @GetMapping(value = DocsApiPaths.DATABASE_SCHEMAS, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequiresArchitectureModelRead
    @Operation(summary = "Every published database schema, with its entity tag",
            description = "The counterpart of the OpenAPI spec index, so that an ERD is only re-rendered when the "
                          + "schema actually changed.")
    @ApiResponse(responseCode = "200", description = "The index",
                    content = @Content(schema = @Schema(implementation = ArtifactIndexDto.class)))
    @ApiResponse(responseCode = "304", description = "If-None-Match matched", content = @Content)
    @ApiResponse(responseCode = "404", description = "The system filter names no known system or alias")
    public ResponseEntity<byte[]> getDatabaseSchemas(
            @Parameter(description = "Restrict the index to one system, by name or alias") @RequestParam(required = false) String system,
            WebRequest request) {
        return respond(request, index(system,
                databaseSchemaRepository::findIndexEntries,
                databaseSchemaRepository::findIndexEntriesBySystemName,
                DocsApiPaths::databaseSchemaContentPath));
    }

    private ArtifactIndexDto index(String systemFilter,
                                   Supplier<List<ArtifactIndexEntry>> findAll,
                                   Function<String, List<ArtifactIndexEntry>> findBySystem,
                                   // BinaryOperator rather than BiFunction<String, String, String>: both
                                   // arguments and the result are Strings, and Sonar (java:S4276) asks for the
                                   // more specialised interface
                                   BinaryOperator<String> contentPath) {
        List<ArtifactIndexEntry> entries = StringUtils.hasText(systemFilter)
                ? findBySystem.apply(resolveSystemName(systemFilter))
                : findAll.get();
        return new ArtifactIndexDto(entries.stream()
                .map(entry -> new ArtifactIndexEntryDto(
                        entry.getSystem(),
                        entry.getComponent(),
                        entry.getVersion(),
                        etagSupport.entityTag(entry.getContentHash()),
                        entry.getLastModifiedAt(),
                        contentPath.apply(entry.getSystem(), entry.getComponent())))
                .toList());
    }

    /**
     * Resolves the {@code system} filter the way every other resource resolves a system - by name or alias,
     * ignoring case - and rejects an unknown one. Matching the stored name alone would answer an alias with an
     * empty index and no hint, and a consumer would silently replicate nothing.
     */
    private String resolveSystemName(String systemFilter) {
        return systemRepository.findByNameOrAliasIgnoreCase(systemFilter)
                .map(System::getName)
                .orElseThrow(() -> DocsApiException.systemNotFound(systemFilter));
    }

    private ResponseEntity<byte[]> respond(WebRequest request, ArtifactIndexDto body) {
        byte[] serialized = etagSupport.serialize(body);
        String entityTag = etagSupport.entityTagOf(serialized);
        if (etagSupport.isNotModified(request, entityTag)) {
            return null;
        }
        return ResponseEntity.ok()
                .eTag(entityTag)
                .cacheControl(CacheControl.noCache())
                .body(serialized);
    }
}
