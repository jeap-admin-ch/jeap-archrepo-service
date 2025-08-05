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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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

    private final SystemComponentRepository systemComponentRepository;

    private final OpenApiSpecRepository openApiSpecRepository;

    private final RestApiRepository restApiRepository;

    private final OpenApiImporter openApiImporter;

    private final SystemComponentService systemComponentService;

    @Transactional
    @PostMapping(value = "/{systemName}/{systemComponentName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an OpenApi Spec",
            requestBody = @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)))
    public ResponseEntity<String> handleFileUploadWithSystemName(
            @PathVariable("systemName") @Size(max = SYSTEM_MAX_LENGTH) String ignored,
            @PathVariable("systemComponentName") @Size(max = COMPONENT_MAX_LENGTH) String systemComponentName,
            @RequestParam(name = "version", required = false) @Size(max = VERSION_MAX_LENGTH) String version,
            @RequestParam(name = "file") MultipartFile file) throws IOException {
        return handleFileUpload(systemComponentName, version, file);
    }

    @Transactional
    @PostMapping(value = "/{systemComponentName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an OpenApi Spec",
            requestBody = @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)))
    public ResponseEntity<String> handleFileUpload(
            @PathVariable("systemComponentName") @Size(max = COMPONENT_MAX_LENGTH) String systemComponentName,
            @RequestParam(name = "version", required = false) @Size(max = VERSION_MAX_LENGTH) String version,
            @RequestParam(name = "file") MultipartFile file) throws IOException {
        try {
            SystemComponent systemComponent = systemComponentService.findOrCreateSystemComponent(systemComponentName);
            System system = systemComponent.getParent();

            Optional<OpenApiSpec> openApiSpecOptional = openApiSpecRepository.findByProvider(systemComponent);

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
    public String getOpenApiJsonWithSystemName(
            @PathVariable("systemName") @Size(max = SYSTEM_MAX_LENGTH) String ignored,
            @PathVariable("systemComponentName") @Size(max = COMPONENT_MAX_LENGTH) String systemComponentName) {
        return getOpenApiJson(systemComponentName);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{systemComponentName}")
    @Operation(summary = "Get the OpenApi Spec of a systemComponent")
    public String getOpenApiJson(
            @PathVariable("systemComponentName") @Size(max = COMPONENT_MAX_LENGTH) String systemComponentName) {
        log.info("Retrieve openApiSpec for systemComponent {}", systemComponentName);

        SystemComponent systemComponent = systemComponentRepository.findByNameIgnoreCase(systemComponentName)
                .orElseThrow(() -> OpenApiException.systemComponentNotExists(systemComponentName));

        Optional<OpenApiSpec> openApiSpecOptional = openApiSpecRepository.findByProvider(systemComponent);

        if (openApiSpecOptional.isEmpty()) {
            throw new IllegalStateException("No Open API spec found for system component " + systemComponentName);
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
    public ResponseEntity<RestApiResultDto> getRestApiForServiceWithSystemName(
            @PathVariable("systemName") @Size(max = SYSTEM_MAX_LENGTH) String ignored,
            @PathVariable("systemComponentName") @Size(max = COMPONENT_MAX_LENGTH) String systemComponentName) {
        return getRestApiForService(systemComponentName);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{systemComponentName}/rest-apis")
    @Operation(
            summary = "All rest apis imported from open api spec",
            description = "Get all rest apis imported from open api spec for a certain system component. Use /api/model to discover available systems and components."
    )
    public ResponseEntity<RestApiResultDto> getRestApiForService(
            @PathVariable("systemComponentName") @Size(max = COMPONENT_MAX_LENGTH) String systemComponentName) {
        log.info("Retrieve rest apis for system component {}", systemComponentName);

        SystemComponent systemComponent = systemComponentRepository.findByNameIgnoreCase(systemComponentName)
                .orElseThrow(() -> OpenApiException.systemComponentNotExists(systemComponentName));

        Optional<ApiDocDto> apiDocVersion = openApiSpecRepository.getApiDocVersion(systemComponent);

        if (apiDocVersion.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<RestApiDto> restApis = restApiRepository.findByProvider(systemComponent)
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
