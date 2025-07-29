package ch.admin.bit.jeap.archrepo.importer.openapi;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.RestApiRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenApiImporterTest {

    @Mock
    private RestApiRepository restApiRepository;

    @Test
    void importIntoModel_mcsExample_allApisLoaded() {
        doImportTest("jme-mcs.json", 7);
        verify(restApiRepository).deleteAll(List.of());
    }

    @Test
    void importIntoModel_wvsExample_allApisLoaded() {
        doImportTest("test-communication-scs.json", 14);
        verify(restApiRepository).deleteAll(List.of());
    }

    @Test
    void importIntoModel_mcsExample_onlyNewApisLoaded() {
        List<RestApi> apis = new ArrayList<>();
        apis.add(new RestApi(mock(SystemComponent.class), "PUT", "/api/contracts/{appName}/{appVersion}", Importer.OPEN_API));
        apis.add(new RestApi(mock(SystemComponent.class), "DELETE", "/api/contracts/{appName}/{appVersion}", Importer.GRAFANA));
        when(restApiRepository.findByProvider(any())).thenReturn(apis);
        doImportTest("jme-mcs.json", 5);
        verify(restApiRepository).deleteAll(List.of());
    }

    @Test
    void importIntoModel_wvsExample_onlyNewApisLoaded() {
        List<RestApi> apis = new ArrayList<>();
        apis.add(new RestApi(mock(SystemComponent.class), "POST", "/ui-api/messages/{id}/revalidate", Importer.OPEN_API));
        apis.add(new RestApi(mock(SystemComponent.class), "POST", "/ui-api/messages/{id}/recreate", Importer.GRAFANA));
        apis.add(new RestApi(mock(SystemComponent.class), "GET", "/api/v2/messages/{id}", Importer.GRAFANA));
        apis.add(new RestApi(mock(SystemComponent.class), "GET", "/api/testsupport/codelists/{code}", Importer.GRAFANA));
        when(restApiRepository.findByProvider(any())).thenReturn(apis);
        doImportTest("test-communication-scs.json", 10);
        verify(restApiRepository).deleteAll(List.of());
    }

    @Test
    void importIntoModel_mcsExample_oldApisDeleted() {
        List<RestApi> apis = new ArrayList<>();
        apis.add(new RestApi(mock(SystemComponent.class), "POST", "/foo/bar", Importer.OPEN_API));
        apis.add(new RestApi(mock(SystemComponent.class), "GET", "/foo/bar", Importer.OPEN_API));
        apis.add(new RestApi(mock(SystemComponent.class), "GET", "/foo/bar/foo", Importer.OPEN_API));
        apis.add(new RestApi(mock(SystemComponent.class), "GET", "/foo/foo", Importer.GRAFANA));
        apis.add(new RestApi(mock(SystemComponent.class), "GET", "/bar/bar", Importer.GRAFANA));
        apis.get(2).addImporter(Importer.GRAFANA);
        when(restApiRepository.findByProvider(any())).thenReturn(apis);
        doImportTest("jme-mcs.json", 7);
        verify(restApiRepository).deleteAll(List.of(apis.get(0), apis.get(1)));
        assertThat(apis.get(2).getImporters()).containsExactly(Importer.GRAFANA);
    }

    @Test
    void importIntoModel_wvsExample_oldApisDeleted() {
        List<RestApi> apis = new ArrayList<>();
        apis.add(new RestApi(mock(SystemComponent.class), "POST", "/foo/bar", Importer.OPEN_API));
        apis.add(new RestApi(mock(SystemComponent.class), "GET", "/foo/bar", Importer.OPEN_API));
        apis.add(new RestApi(mock(SystemComponent.class), "GET", "/foo/bar/foo", Importer.OPEN_API));
        apis.add(new RestApi(mock(SystemComponent.class), "GET", "/foo/foo", Importer.GRAFANA));
        apis.add(new RestApi(mock(SystemComponent.class), "GET", "/bar/bar", Importer.GRAFANA));
        apis.get(2).addImporter(Importer.GRAFANA);
        when(restApiRepository.findByProvider(any())).thenReturn(apis);
        doImportTest("test-communication-scs.json", 14);
        verify(restApiRepository).deleteAll(List.of(apis.get(0), apis.get(1)));
        assertThat(apis.get(2).getImporters()).containsExactly(Importer.GRAFANA);
    }

    @SneakyThrows
    private void doImportTest(String fileName, int count) {
        OpenApiImporter openApiImporter = new OpenApiImporter(new ObjectMapper(), restApiRepository);
        ClassPathResource classPathResource = new ClassPathResource("openapi/" + fileName);
        byte[] openApiSpecFileContent = Files.readAllBytes(classPathResource.getFile().toPath());
        SystemComponent systemComponent = BackendService.builder().name("junit").build();
        openApiImporter.importIntoModel(systemComponent, openApiSpecFileContent);
        verify(restApiRepository, times(count)).save(any());
    }

    @Test
    @SuppressWarnings("java:S5778")
    void importIntoModel_wrongContent_exceptionThrown() {
        OpenApiImporter openApiImporter = new OpenApiImporter(new ObjectMapper(), restApiRepository);
        SystemComponent systemComponent = BackendService.builder().name("junit").build();
        assertThrows(OpenApiFileParsingException.class,
                () -> openApiImporter.importIntoModel(systemComponent, "dummy".getBytes()));
    }

    @SneakyThrows
    @Test
    void getServerUrl_returnServerUrl() {
        OpenApiImporter openApiImporter = new OpenApiImporter(new ObjectMapper(), restApiRepository);
        ClassPathResource classPathResource = new ClassPathResource("openapi/test-communication-scs.json");
        byte[] openApiSpecFileContent = Files.readAllBytes(classPathResource.getFile().toPath());
        assertThat(openApiImporter.getServerUrl(openApiSpecFileContent)).isEqualTo("https://some-url/communication");
    }

}
