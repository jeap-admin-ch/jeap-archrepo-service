package ch.admin.bit.jeap.archrepo.importer.openapi;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.RestApiRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class OpenApiImporter {

    private final ObjectMapper objectMapper;
    private final RestApiRepository restApiRepository;

    @Transactional
    public void importIntoModel(SystemComponent systemComponent, byte[] openApiSpecFileContent) {
        log.info("Importing into OpenAPI model");

        List<RestApi> restApisFromFile = readEndpointsFromOpenApiFile(systemComponent, openApiSpecFileContent);
        List<RestApi> currentRestApis = restApiRepository.findByDefiningSystemAndProvider(systemComponent.getParent(), systemComponent);
        List<RestApi> toDelete = filterRestApisToDelete(currentRestApis);

        updateOrAddRestApis(restApisFromFile, currentRestApis, toDelete);
        deleteOldRestApis(toDelete);
    }

    private void updateOrAddRestApis(List<RestApi> restApisFromFile, List<RestApi> currentRestApis, List<RestApi> toDelete) {
        for (RestApi restApi : restApisFromFile) {
            Optional<RestApi> currentRestApi = findMatchingRestApi(restApi, currentRestApis);
            if (currentRestApi.isPresent()) {
                log.info("REST API already defined: {} {}", restApi.getMethod(), restApi.getPath());
                currentRestApi.get().addImporter(Importer.OPEN_API);
                toDelete.remove(currentRestApi.get());
            } else {
                log.info("REST API not defined: {} {}", restApi.getMethod(), restApi.getPath());
                restApiRepository.save(restApi);
            }
        }

        markRestApisForDeletion(toDelete);
    }

    private void markRestApisForDeletion(List<RestApi> toDelete) {
        toDelete.removeIf(restApi -> {
            restApi.removeImporter(Importer.OPEN_API);
            return !restApi.getImporters().isEmpty();
        });
    }

    @SuppressWarnings("java:S6204")
    private List<RestApi> filterRestApisToDelete(List<RestApi> currentRestApis) {
        return currentRestApis.stream()
                .filter(current -> current.getImporters().contains(Importer.OPEN_API))
                .collect(Collectors.toList());
    }

    private void deleteOldRestApis(List<RestApi> toDelete) {
        log.info("Deleting old REST APIs, count: {}", toDelete.size());
        toDelete.forEach(restApi -> log.info("REST API deleted: {} {}", restApi.getMethod(), restApi.getPath()));
        restApiRepository.deleteAll(toDelete);
    }

    private Optional<RestApi> findMatchingRestApi(RestApi restApi, List<RestApi> currentRestApis) {
        return currentRestApis.stream()
                .filter(current -> current.getPath().equals(restApi.getPath()) && current.getMethod().equals(restApi.getMethod()))
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    private List<RestApi> readEndpointsFromOpenApiFile(SystemComponent systemComponent, byte[] openApiSpecFileContent) {
        log.info("Parsing OpenAPI file...");
        List<RestApi> restApis = new ArrayList<>();
        try {
            Map<String, Object> openApi = objectMapper.readValue(openApiSpecFileContent, new TypeReference<>() {});
            Map<String, Object> paths = (Map<String, Object>) openApi.get("paths");

            paths.forEach((path, methods) ->
                ((Map<String, Object>) methods).forEach((method, value) -> {
                    log.debug("Found path: {} {}", method, path);
                    restApis.add(new RestApi(systemComponent, method, path, Importer.OPEN_API));
                }));
        } catch (Exception e) {
            String message = "Failed to parse the provided OpenAPI file to retrieve the rest apis";
            log.warn(message, e);
            throw new OpenApiFileParsingException(message, e);
        }
        return restApis;
    }

    @SuppressWarnings("unchecked")
    public String getServerUrl(byte[] openApiSpecFileContent) {
        log.info("Get Server URL from OpenAPI file...");
        try {
            Map<String, Object> openApi = objectMapper.readValue(openApiSpecFileContent, new TypeReference<>() {});
            List<Map<String, String>> servers = (List<Map<String, String>>) openApi.get("servers");
            String url = servers.getFirst().get("url");
            log.info("Found server url: {}", url);
            return url;
        } catch (Exception e) {
            String message = "Failed to parse the provided OpenAPI file to retrieve the server url";
            log.warn(message, e);
            throw new OpenApiFileParsingException(message, e);
        }
    }
}
