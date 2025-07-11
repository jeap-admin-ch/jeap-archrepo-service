package ch.admin.bit.jeap.archrepo.web.rest.openapi;

import ch.admin.bit.jeap.archrepo.importer.openapi.OpenApiImporter;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.*;
import ch.admin.bit.jeap.archrepo.web.rest.model.RestApiDto;
import ch.admin.bit.jeap.archrepo.web.rest.model.RestApiResultDto;
import ch.admin.bit.jeap.archrepo.web.service.SystemComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/openapi")
@RequiredArgsConstructor
@Tag(name = "openapi", description = "OpenAPI Repository")
@Slf4j
class OpenApiController {

    private static final int SYSTEM_MAX_LENGTH = 200;
    private static final int COMPONENT_MAX_LENGTH = 200;
    private static final int VERSION_MAX_LENGTH = 200;

    @Value("${archrepo.openapi-base-url}")
    private String openApiBaseUrl;

    private final SystemRepository systemRepository;

    private final OpenApiSpecRepository openApiSpecRepository;

    private final RestApiRepository restApiRepository;

    private final OpenApiImporter openApiImporter;

    private final SystemComponentService systemComponentService;

    @Transactional
    @PostMapping("/{systemName}/{systemComponentName}")
    @Operation(summary = "Upload an OpenApi Spec")
    public ResponseEntity<String> handleFileUpload(
            @PathVariable @Size(max = SYSTEM_MAX_LENGTH) String systemName,
            @PathVariable @Size(max = COMPONENT_MAX_LENGTH) String systemComponentName,
            @RequestParam(required = false) @Size(max = VERSION_MAX_LENGTH) String version,
            @RequestParam MultipartFile file) throws IOException {
        try {
            SystemComponent systemComponent = systemComponentService.findOrCreateSystemComponent(systemComponentName);
            System system = systemComponent.getParent();

            Optional<OpenApiSpec> openApiSpecOptional = openApiSpecRepository.findByDefiningSystemAndProvider(system, systemComponent);

            String serverUrl = openApiImporter.getServerUrl(file.getBytes());

            if (openApiSpecOptional.isEmpty()) {
                OpenApiSpec openApiSpec = OpenApiSpec.builder()
                        .provider(systemComponent)
                        .version(version)
                        .content(file.getBytes())
                        .serverUrl(serverUrl)
                        .build();
                log.info("Save new openApiSpec for system {} and systemComponent {} as {}",
                        system.getName(), systemComponent.getName(), openApiSpec);
                openApiSpecRepository.save(openApiSpec);
            } else {
                OpenApiSpec openApiSpec = openApiSpecOptional.get();
                openApiSpec.update(file.getBytes(), version, serverUrl);
                log.info("Update openApiSpec for system {} and systemComponent {} with new content as {}",
                        system.getName(), systemComponent.getName(), openApiSpec);
            }
            openApiImporter.importIntoModel(systemComponent, file.getBytes());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (OpenApiException openApiException) {
            log.warn("Error in OpenApi upload: {}", openApiException.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OpenApi upload failed", openApiException);
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/{systemName}/{systemComponentName}")
    @Operation(summary = "Get the OpenApi Spec of a systemComponent")
    public String getOpenApiJson(
            @PathVariable @Size(max = SYSTEM_MAX_LENGTH) String systemName,
            @PathVariable @Size(max = COMPONENT_MAX_LENGTH) String systemComponentName) {
        log.info("Retrieve openApiSpec for system {} and systemComponent {}", systemName, systemComponentName);

        System system = systemRepository.findByNameContainingIgnoreCase(systemName)
                .orElseThrow(() -> OpenApiException.systemNotExists(systemName));

        SystemComponent systemComponent = system.findSystemComponent(systemComponentName)
                .orElseThrow(() -> OpenApiException.systemComponentNotExists(systemComponentName, systemName));

        Optional<OpenApiSpec> openApiSpecOptional = openApiSpecRepository.findByDefiningSystemAndProvider(system, systemComponent);

        if (openApiSpecOptional.isEmpty()) {
            throw new IllegalStateException("No OpenApiSpec found for system " + systemName + " and systemComponent " + systemComponentName);
        }

        log.debug("Found openApiSpec {} ", openApiSpecOptional.get());
        return new String(openApiSpecOptional.get().getContent(), StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    @GetMapping(value = "/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the API documentation versions of all system components.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The API documentation versions of all system components.",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public List<ApiDocVersion> getApiDocVersions() {
        log.debug("Retrieving API documentation versions.");
        List<ApiDocVersion> apiDocVersions = openApiSpecRepository.getApiDocVersions();
        log.debug("Returning API documentation versions for '{}' system components", apiDocVersions.size());
        return apiDocVersions;
    }

    @Transactional(readOnly = true)
    @GetMapping("/{systemName}/{systemComponentName}/rest-apis")
    @Operation(
            summary = "All rest apis imported from open api spec",
            description = "Get all rest apis imported from open api spec for a certain system and component. Use /api/model to discover available systems and components."
    )
    public ResponseEntity<RestApiResultDto> getRestApiForService(
            @PathVariable @Size(max = SYSTEM_MAX_LENGTH) String systemName,
            @PathVariable @Size(max = COMPONENT_MAX_LENGTH) String systemComponentName) {
        log.info("Retrieve rest apis for system {} and systemComponent {}", systemName, systemComponentName);

        System system = systemRepository.findByNameContainingIgnoreCase(systemName)
                .orElseThrow(() -> OpenApiException.systemNotExists(systemName));

        SystemComponent systemComponent = system.findSystemComponent(systemComponentName)
                .orElseThrow(() -> OpenApiException.systemComponentNotExists(systemComponentName, systemName));

        Optional<ApiDocDto> apiDocVersion = openApiSpecRepository.getApiDocVersion(system, systemComponent);

        if (apiDocVersion.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<RestApiDto> restApis = restApiRepository.findByDefiningSystemAndProvider(systemComponent.getParent(), systemComponent)
                .stream().filter(r -> r.getImporters().contains(Importer.OPEN_API)).map(r -> new RestApiDto(r.getMethod(), r.getPath())).toList();

        log.info("Found {} rest apis with OPEN_API importer", restApis.size());
        return ResponseEntity.ok(RestApiResultDto.of(apiDocVersion.get(), restApis));
    }

    @PostConstruct
    void setupOpenApiBaseUrl() {
        if ((openApiBaseUrl == null) || (openApiBaseUrl.isEmpty()))
            throw OpenApiException.openApiBaseUrlIsNotSet();
    }
}
