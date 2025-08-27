package ch.admin.bit.jeap.archrepo.web.rest.openapi;

import ch.admin.bit.jeap.archrepo.importer.openapi.OpenApiImporter;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.*;
import ch.admin.bit.jeap.archrepo.web.ArchRepoApplication;
import ch.admin.bit.jeap.archrepo.web.rest.model.RestApiResultDto;
import ch.admin.bit.jeap.archrepo.web.service.SystemComponentService;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import ch.admin.bit.jeap.security.test.jws.JwsBuilderFactory;
import ch.admin.bit.jeap.security.test.resource.configuration.JeapOAuth2IntegrationTestResourceConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = ArchRepoApplication.class,
        properties = {  "server.port=8901",
                "jeap.security.oauth2.resourceserver.authorization-server.issuer=" + JwsBuilder.DEFAULT_ISSUER,
                "jeap.security.oauth2.resourceserver.authorization-server.jwk-set-uri=http://localhost:${server.port}/test-app/.well-known/jwks.json"})
@Import(JeapOAuth2IntegrationTestResourceConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiControllerTests {

    private static final String SYSTEM = "system";
    private static final String SERVICE = "system-context-service";
    private static final String UPLOAD_BASE_PATH = "/api/openapi/";
    private static final String UPLOAD_PATH = UPLOAD_BASE_PATH + SERVICE;
    private static final String VERSION = "1.2.3";
    private static final String VERSION_V2 = "2.0.0";
    private static final byte[] CONTENT = "some content".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CONTENT_V2 = "some updated content".getBytes(StandardCharsets.UTF_8);
    private static final String GET_VERSIONS_PATH = "/api/openapi/versions";
    private static final RequestPostProcessor BASIC_AUTH = SecurityMockMvcRequestPostProcessors.httpBasic("api", "secret");

    private static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
    private static final JeapAuthenticationContext CONTEXT = JeapAuthenticationContext.SYS;

    private static final SemanticApplicationRole OPEN_API_DOC_READ_ROLE = SemanticApplicationRole.builder()
            .system("application-platform")
            .resource("openapidoc")
            .operation("read")
            .build();
    private static final SemanticApplicationRole OPEN_API_DOC_WRITE_ROLE = SemanticApplicationRole.builder()
            .system("application-platform")
            .resource("openapidoc")
            .operation("write")
            .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwsBuilderFactory jwsBuilderFactory;

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
    void testUploadOpenApiDocumentation_WhenCreatedWithCorrectBearerAuth_ThenRespondsWithCreated() throws Exception {
        mockSystemComponent();
        final String bearerAuth = createBearerAuthForUserRoles(OPEN_API_DOC_WRITE_ROLE);
        MockMultipartFile file = createMockMultipartFile();

        mockMvc.perform(multipart(UPLOAD_PATH + "?version=" + VERSION)
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, bearerAuth)
        ).andExpect(status().isCreated());

        OpenApiSpec openApiSpec = openApiSpecArgumentCaptor.getValue();
        assertThat(openApiSpec.getDefiningSystem().getName()).isEqualTo(SYSTEM);
        assertThat(openApiSpec.getProvider().getName()).isEqualTo(SERVICE);
        assertThat(openApiSpec.getContent()).isEqualTo(CONTENT);
        assertThat(openApiSpec.getVersion()).isEqualTo(VERSION);
        verify(openApiImporter, times(1)).importIntoModel(any(SystemComponent.class), eq(CONTENT));
    }

    @Test
    void testUploadOpenApiDocumentation_WhenUpdatedWithCorrectBearerAuth_ThenRespondsWithOK() throws Exception {
        SystemComponent systemComponent = mockSystemComponent();
        OpenApiSpec openApiSpec = mock(OpenApiSpec.class);
        when(openApiSpecRepository.findByProvider(systemComponent)).thenReturn(Optional.of(openApiSpec));
        when(openApiImporter.getServerUrl(CONTENT_V2)).thenReturn("test-server-url");
        final String bearerAuth = createBearerAuthForUserRoles(OPEN_API_DOC_WRITE_ROLE);
        MockMultipartFile fileV2 = createMockMultipartFile(CONTENT_V2);

        mockMvc.perform(multipart(UPLOAD_PATH + "?version=" + VERSION_V2)
                .file(fileV2)
                .header(HttpHeaders.AUTHORIZATION, bearerAuth)

        ).andExpect(status().isOk());

        verify(openApiSpec, times(1)).update(CONTENT_V2, VERSION_V2, "test-server-url");
        verify(openApiImporter, times(1)).importIntoModel(any(SystemComponent.class), eq(CONTENT_V2));
    }

    @Test
    void testUploadOpenApiSpec_WhenVersionTooLong_ThenRespondsWithBadRequest() throws Exception {
        MockMultipartFile file = createMockMultipartFile();
        final String bearerAuth = createBearerAuthForUserRoles(OPEN_API_DOC_WRITE_ROLE);
        final String tooLongVersion = "v".repeat(201);

        mockMvc.perform(multipart(UPLOAD_PATH + "?version=" + tooLongVersion)
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, bearerAuth)
        ).andExpect(status().isBadRequest());
        verify(openApiImporter, never()).importIntoModel(any(), any());
    }

    @Test
    void testUploadOpenApiSpec_WhenComponentNameTooLong_ThenRespondsWithBadRequest() throws Exception {
        MockMultipartFile file = createMockMultipartFile();
        final String bearerAuth = createBearerAuthForUserRoles(OPEN_API_DOC_WRITE_ROLE);
        final String tooLongComponentName = "n".repeat(201);

        mockMvc.perform(multipart(UPLOAD_BASE_PATH + tooLongComponentName + "?version=" + VERSION)
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, bearerAuth)
        ).andExpect(status().isBadRequest());
        verify(openApiImporter, never()).importIntoModel(any(), any());
    }

    @Test
    void testUploadOpenApi_WhenHasOnlyReadRoleInBearerAuth_ThenRespondsWithForbidden() throws Exception {
        MockMultipartFile file = createMockMultipartFile();
        final String bearerAuth = createBearerAuthForUserRoles(OPEN_API_DOC_READ_ROLE);

        mockMvc.perform(multipart(UPLOAD_PATH + "?version=" + VERSION)
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, bearerAuth)
        ).andExpect(status().isForbidden());
    }

    @Test
    void testUploadOpenApi_WhenHasCorrectBasicAuth_ThenOK() throws Exception {
        MockMultipartFile file = createMockMultipartFile();

        mockSystemComponent();

        mockMvc.perform(multipart(UPLOAD_PATH + "?version=" + VERSION)
                .file(file)
                .with(BASIC_AUTH)
        ).andExpect(status().isCreated());
    }

    @Test
    void testUploadOpenApi_WhenHasNoAuthentication_ThenRespondsWithUnauthorized() throws Exception {
        MockMultipartFile file = createMockMultipartFile();

        mockMvc.perform(multipart(UPLOAD_PATH + "?version=" + VERSION)
                .file(file)
                .with(csrf())
        ).andExpect(status().isUnauthorized());
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
        return createMockMultipartFile(CONTENT);
    }

    private MockMultipartFile createMockMultipartFile(byte[] content) {
        return new MockMultipartFile(
                "file",
                "openapi.json",
                MediaType.APPLICATION_JSON.toString(),
                content
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

    private SystemComponent mockSystemComponent() {
        System system = createSystem();
        SystemComponent systemComponent = mock(SystemComponent.class);
        when(systemComponent.getParent()).thenReturn(system);
        when(systemComponent.getName()).thenReturn(SERVICE);
        when(systemComponentService.findOrCreateSystemComponent(SERVICE)).thenReturn(systemComponent);
        return systemComponent;
    }

    private String createBearerAuthForUserRoles(SemanticApplicationRole... userroles) {
        return "Bearer " + jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, CONTEXT).
                withUserRoles(userroles).
                build().serialize();
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
