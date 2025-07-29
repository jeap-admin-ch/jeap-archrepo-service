package ch.admin.bit.jeap.archrepo.web.rest.openapi;

import ch.admin.bit.jeap.archrepo.importer.openapi.OpenApiImporter;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.*;
import ch.admin.bit.jeap.archrepo.web.config.WebSecurityConfig;
import ch.admin.bit.jeap.archrepo.web.rest.model.ArchRepoWebTestConfiguration;
import ch.admin.bit.jeap.archrepo.web.rest.model.RestApiResultDto;
import ch.admin.bit.jeap.archrepo.web.service.SystemComponentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OpenApiController.class, WebSecurityConfig.class})
@Import(ArchRepoWebTestConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiControllerTests {

    private static final String SYSTEM = "system";
    private static final String SERVICE = "system-context-service";
    private static final String UPLOAD_PATH = "/api/openapi/" + SYSTEM + "/" + SERVICE;
    private static final String VERSION = "1.2.3";
    private static final byte[] CONTENT = "some content".getBytes(StandardCharsets.UTF_8);
    private static final String GET_VERSIONS_PATH = "/api/openapi/versions";
    private static final RequestPostProcessor BASIC_AUTH = SecurityMockMvcRequestPostProcessors.httpBasic("api", "secret");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    SystemComponentRepository systemComponentRepository;

    @MockitoBean
    OpenApiSpecRepository openApiSpecRepository;

    @MockitoBean
    RestApiRepository restApiRepository;

    @MockitoBean
    SystemComponentService systemComponentService;

    @MockitoBean
    OpenApiImporter openApiImporter;

    @Captor
    ArgumentCaptor<OpenApiSpec> openApiSpecArgumentCaptor;

    private System system;

    @BeforeEach
    void setUp() {
        system = createSystem();
        when(openApiSpecRepository.save(openApiSpecArgumentCaptor.capture())).
                thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testUploadOpenApiSpecWithoutVersion() throws Exception {
        mockSystemComponent();

        MockMultipartFile file = createMockMultipartFile();

        mockMvc.perform(multipart(UPLOAD_PATH)
                .file(file)
                .with(BASIC_AUTH)
        ).andExpect(status().isCreated());

        OpenApiSpec openApiSpec = openApiSpecArgumentCaptor.getValue();
        assertThat(openApiSpec.getDefiningSystem().getName()).isEqualTo(SYSTEM);
        assertThat(openApiSpec.getProvider().getName()).isEqualTo(SERVICE);
        assertThat(openApiSpec.getContent()).isEqualTo(CONTENT);
        assertThat(openApiSpec.getVersion()).isNull();
        verify(openApiImporter, times(1)).importIntoModel(any(SystemComponent.class), eq(CONTENT));
    }

    private void mockSystemComponent() {
        System system = createSystem();
        SystemComponent systemComponent = mock(SystemComponent.class);
        when(systemComponent.getParent()).thenReturn(system);
        when(systemComponent.getName()).thenReturn(SERVICE);
        when(systemComponentService.findOrCreateSystemComponent(SERVICE)).thenReturn(systemComponent);
    }

    @Test
    void testUploadOpenApiSpecWithVersion() throws Exception {
        MockMultipartFile file = createMockMultipartFile();

        mockSystemComponent();

        mockMvc.perform(multipart(UPLOAD_PATH + "?version=" + VERSION)
                .file(file)
                .with(BASIC_AUTH)
        ).andExpect(status().isCreated());

        OpenApiSpec openApiSpec = openApiSpecArgumentCaptor.getValue();
        assertThat(openApiSpec.getDefiningSystem().getName()).isEqualTo(SYSTEM);
        assertThat(openApiSpec.getProvider().getName()).isEqualTo(SERVICE);
        assertThat(openApiSpec.getContent()).isEqualTo(CONTENT);
        assertThat(openApiSpec.getVersion()).isEqualTo(VERSION);
        verify(openApiImporter, times(1)).importIntoModel(any(SystemComponent.class), eq(CONTENT));
    }

    @Test
    void testUploadOpenApiSpecWithTooLongVersionFails() throws Exception {
        MockMultipartFile file = createMockMultipartFile();
        final String tooLongVersion = "v".repeat(201);

        mockMvc.perform(multipart(UPLOAD_PATH + "?version=" + tooLongVersion)
                .file(file)
                .with(BASIC_AUTH)
        ).andExpect(status().is4xxClientError());
        verify(openApiImporter, never()).importIntoModel(any(), any());
    }

    @Test
    void testGetApiDocVersions() throws Exception {
        final ApiDocVersionImpl apiDocVersionWithVersion = new ApiDocVersionImpl(SYSTEM, SERVICE, VERSION);
        final ApiDocVersionImpl apiDocVersionWithoutVersion = new ApiDocVersionImpl(SYSTEM, "other-service", null);
        when(openApiSpecRepository.getApiDocVersions()).
                thenReturn(List.of(apiDocVersionWithVersion, apiDocVersionWithoutVersion));

        String json = mockMvc.perform(get(GET_VERSIONS_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<ApiDocVersionImpl> apiDocVersions = objectMapper.readValue(json, new TypeReference<>() {});
        assertThat(apiDocVersions).containsExactly(apiDocVersionWithVersion, apiDocVersionWithoutVersion);
    }

    @Test
    void testGetRestAPIs() throws Exception {
        ZonedDateTime createdAt = ZonedDateTime.now();

        when(systemComponentRepository.findByNameIgnoreCase(SERVICE))
                .thenReturn(system.findSystemComponent(SERVICE));

        when(openApiSpecRepository.getApiDocVersion(any())).
                thenReturn(Optional.of(new ApiDocDtoImpl("serverUrl", VERSION, createdAt, null)));

        when(restApiRepository.findByProvider(any())).thenReturn(
                List.of(
                        RestApi.builder()
                                .provider(BackendService.builder().name("test").build())
                                .path("path1")
                                .method("POST")
                                .importer(Importer.OPEN_API)
                                .build(),
                        RestApi.builder()
                                .provider(BackendService.builder().name("test").build())
                                .path("path2")
                                .method("GET")
                                .importer(Importer.GRAFANA)
                                .build())
        );

        String json = mockMvc.perform(get(UPLOAD_PATH + "/rest-apis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        RestApiResultDto result = objectMapper.readValue(json, RestApiResultDto.class);
        assertThat(result.serverUrl()).isEqualTo("serverUrl");
        assertThat(result.version()).isEqualTo(VERSION);
        assertThat(result.lastUpdated()).isEqualTo(createdAt);
        assertThat(result.restApis()).hasSize(1);
        assertThat(result.restApis().getFirst().method()).isEqualTo("POST");
        assertThat(result.restApis().getFirst().path()).isEqualTo("path1");
    }

    @Test
    void testGetRestAPIs_modifiedAtAsLastUpdated() throws Exception {
        ZonedDateTime createdAt = ZonedDateTime.now().minusDays(1);
        ZonedDateTime modifiedAt = ZonedDateTime.now().plusDays(1);
        when(systemComponentRepository.findByNameIgnoreCase(SERVICE))
                .thenReturn(system.findSystemComponent(SERVICE));
        when(openApiSpecRepository.getApiDocVersion(any())).
                thenReturn(Optional.of(new ApiDocDtoImpl("serverUrl", VERSION, createdAt, modifiedAt)));

        RestApi restApi = RestApi.builder()
                .provider(BackendService.builder().name("test").build())
                .path("path1")
                .method("POST")
                .importer(Importer.PACT_BROKER)
                .build();

        restApi.addImporter(Importer.OPEN_API);

        when(restApiRepository.findByProvider(any())).thenReturn(List.of(restApi));

        String json = mockMvc.perform(get(UPLOAD_PATH + "/rest-apis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        RestApiResultDto result = objectMapper.readValue(json, RestApiResultDto.class);
        assertThat(result.version()).isEqualTo(VERSION);
        assertThat(result.lastUpdated()).isEqualTo(modifiedAt);
        assertThat(result.restApis()).hasSize(1);
        assertThat(result.restApis().getFirst().method()).isEqualTo("POST");
        assertThat(result.restApis().getFirst().path()).isEqualTo("path1");
    }

    @Test
    void testGetRestAPIs_notFound() throws Exception {
        when(systemComponentRepository.findByNameIgnoreCase(SERVICE))
                .thenReturn(system.findSystemComponent(SERVICE));

        mockMvc.perform(get(UPLOAD_PATH + "/rest-apis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetRestAPIs_serviceNotFound_badRequest() throws Exception {
        mockMvc.perform(get("/api/openapi/" + SYSTEM + "/foo/rest-apis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetRestAPIs_systemNotFound_badRequest() throws Exception {
        mockMvc.perform(get("/api/openapi/foo/" + SERVICE + "/rest-apis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private MockMultipartFile createMockMultipartFile() {
        return new MockMultipartFile(
                "file",
                "openapi.json",
                MediaType.APPLICATION_JSON.toString(),
                CONTENT
        );
    }

    private System createSystem() {
        System system = System.builder()
                .name(SYSTEM)
                .build();
        BackendService backendService = BackendService.builder()
                .name(SERVICE)
                .build();
        system.addSystemComponent(backendService);
        return system;
    }

    @Value
    private static class ApiDocVersionImpl implements ApiDocVersion {
        String system;
        String component;
        String version;
    }

    @Value
    private static class ApiDocDtoImpl implements ApiDocDto {
        String serverUrl;
        String version;
        ZonedDateTime createdAt;
        ZonedDateTime modifiedAt;
    }

}
