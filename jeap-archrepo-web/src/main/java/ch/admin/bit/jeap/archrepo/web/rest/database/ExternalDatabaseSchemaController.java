package ch.admin.bit.jeap.archrepo.web.rest.database;

import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.model.database.DatabaseSchema;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentDatabaseSchemaRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/external-api/dbschemas")
@Tag(name = "external-dbschemas", description = "External access to database schema definitions associated with system components")
@RequiredArgsConstructor
class ExternalDatabaseSchemaController {

    private final SystemRepository systemRepository;
    private final SystemComponentDatabaseSchemaRepository systemComponentDatabaseSchemaRepository;

    @Transactional
    @GetMapping(produces = "application/json")
    @Operation(
            summary = "Get the database schema definition associated with a system component."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Database schema retrieved successfully.",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = DatabaseSchemaDto.class))),
            @ApiResponse(responseCode = "404", description = "No database schema found for the specified system component, or queried system or system component don't exist."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    @PreAuthorize("hasRole(#systemName,'external-database-schema', 'read')")
    public ResponseEntity<DatabaseSchemaDto> getDatabaseSchema(
            @Parameter(description = "Name of the system to retrieve the database schema for", required = true)
            @RequestParam String systemName,
            @Parameter(description = "Name of the system component to retrieve the database schema for", required = true)
            @RequestParam String systemComponentName) {
        try {
            SystemComponentDatabaseSchema systemComponentDatabaseSchema = getSystemComponentDatabaseSchema(systemName, systemComponentName);
            if (systemComponentDatabaseSchema == null) {
                log.debug("No database schema found for the system component '{}' in the system '{}'.", systemComponentName, systemName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else {
                DatabaseSchemaDto schemaDto = DatabaseSchemaDto.builder()
                        .systemName(systemName)
                        .systemComponentName(systemComponentName)
                        .schema(getDatabaseSchema(systemComponentDatabaseSchema))
                        .build();
                return ResponseEntity.ok(schemaDto);
            }
        } catch (Exception e) {
            log.error("Unexpected error while fetching the database schema for the component '{}' in the system '{}'.", systemName, systemName, e);
            throw DatabaseSchemaException.unexpectedError(systemName, systemComponentName, e);
        }
    }

    private SystemComponentDatabaseSchema getSystemComponentDatabaseSchema(String systemName, String systemComponentName) {
        return systemRepository.findByNameContainingIgnoreCase(systemName)
                .flatMap(system -> system.findSystemComponent(systemComponentName))
                .flatMap(systemComponentDatabaseSchemaRepository::findBySystemComponent)
                .orElse(null);
    }

    private DatabaseSchema getDatabaseSchema(SystemComponentDatabaseSchema systemComponentDatabaseSchema) {
        try {
            return DatabaseSchema.fromJson(systemComponentDatabaseSchema.getSchema());
        } catch (Exception e) {
            throw DatabaseSchemaException.schemaDeserializationError(
                    systemComponentDatabaseSchema.getSystem().getName(),
                    systemComponentDatabaseSchema.getSystemComponent().getName(),
                    e);
        }
    }

}
