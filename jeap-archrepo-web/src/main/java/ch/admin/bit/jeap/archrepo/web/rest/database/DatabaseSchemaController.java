package ch.admin.bit.jeap.archrepo.web.rest.database;

import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentDatabaseSchemaRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.function.Supplier;

@RestController
@RequestMapping(value = "/api/dbschemas")
@RequiredArgsConstructor
@Tag(name = "dbschemas", description = "Manage the data base schema definitions associated with system components")
@Slf4j
class DatabaseSchemaController {

    private final SystemComponentRepository systemComponentRepository;
    private final SystemComponentDatabaseSchemaRepository systemComponentDatabaseSchemaRepository;
    private final PlatformTransactionManager transactionManager;

    @Transactional
    @PostMapping(consumes = "application/json")
    @Operation(summary = "Create or update the database schema definition associated with a system component.")

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "The database schema definition to create or update.",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CreateOrUpdateDbSchemaDto.class)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Database schema created successfully."),
            @ApiResponse(responseCode = "200", description = "Database schema updated successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid input data, e.g. system or system component do not exist or database schema not valid."),
            @ApiResponse(responseCode = "500", description = "Unexpected server error.")
    })
    @PreAuthorize("hasRole('database-schema', 'write')")
    public ResponseEntity<Void> createOrUpdateDatabaseSchema(@Valid @RequestBody CreateOrUpdateDbSchemaDto schemaDto) {
        try {
            SystemComponent systemComponent = systemComponentRepository.findByNameContainingIgnoreCase(schemaDto.getSystemComponentName())
                    .orElseThrow(() -> DatabaseSchemaException.systemComponentDoesNotExist(schemaDto.getSystemComponentName()));

            return updateDatabaseSchema(schemaDto, systemComponent).
                    orElseGet(() -> createDatabaseSchema(schemaDto, systemComponent));
        } catch (DatabaseSchemaException e) {
            log.error("Updating or creating the database schema '{}' failed.", schemaDto, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while updating or creating the database schema '{}'.", schemaDto, e);
            throw DatabaseSchemaException.unexpectedError(schemaDto, e);
        }
    }

    private Optional<ResponseEntity<Void>> updateDatabaseSchema(CreateOrUpdateDbSchemaDto schemaDto, SystemComponent systemComponent) {
        return systemComponentDatabaseSchemaRepository.findBySystemComponent(systemComponent)
                .map(databaseSchema ->
                        databaseSchema.update(getJsonSchema(schemaDto), schemaDto.getSchema().version()))
                .map(databaseSchema -> ResponseEntity.status(HttpStatus.OK).build());
    }

    private ResponseEntity<Void> createDatabaseSchema(CreateOrUpdateDbSchemaDto schemaDto, SystemComponent systemComponent) {
        SystemComponentDatabaseSchema databaseSchema = SystemComponentDatabaseSchema.builder()
                .systemComponent(systemComponent)
                .schema(getJsonSchema(schemaDto))
                .schemaVersion(schemaDto.getSchema().version())
                .build();
        try {
            systemComponentDatabaseSchemaRepository.saveAndFlush(databaseSchema);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (DataIntegrityViolationException dive) {
            // Another request created the database schema in the meantime.
            return withinNewTransactionWithResult( () ->
                        updateDatabaseSchema(schemaDto, systemComponent)
                        .orElseThrow(() -> DatabaseSchemaException.unexpectedError(schemaDto,
                            "Expected the database schema to be present.")));
        }
    }

    private byte[] getJsonSchema(CreateOrUpdateDbSchemaDto schemaDto) {
        try {
            return schemaDto.getSchema().toJson();
        } catch (Exception e) {
            log.error("Failed to serialize the schema {} to JSON.", schemaDto, e);
            throw DatabaseSchemaException.schemaSerializationError(schemaDto, e);
        }
    }

    private <V> V withinNewTransactionWithResult(Supplier<V> callback) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> callback.get());
    }

}
