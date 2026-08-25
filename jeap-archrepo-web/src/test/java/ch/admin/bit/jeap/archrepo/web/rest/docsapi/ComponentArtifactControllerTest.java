package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.ContentHash;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.model.database.DatabaseSchema;
import ch.admin.bit.jeap.archrepo.model.database.Table;
import ch.admin.bit.jeap.archrepo.model.database.TableColumn;
import ch.admin.bit.jeap.archrepo.persistence.OpenApiSpecRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentDatabaseSchemaRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.web.config.WebSecurityConfig;
import ch.admin.bit.jeap.security.resource.configuration.MvcSecurityConfiguration;
import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import ch.admin.bit.jeap.security.resource.token.TokenConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ComponentArtifactController.class,
        properties = "archrepo.openapi-base-url=https://archrepo.example.com/archrepo-service/api/openapi/")
@Import({DocsApiTestConfiguration.class, WebSecurityConfig.class,
        MvcSecurityConfiguration.class, ResourceServerProperties.class, TokenConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComponentArtifactControllerTest extends DocsApiControllerTestBase {

    private static final String SYSTEM = "wvs";
    private static final String COMPONENT = "wvs-foo-bar-service";
    private static final byte[] SPEC = "{\"openapi\":\"3.0.0\"}".getBytes(StandardCharsets.UTF_8);
    private static final String OPENAPI_PATH = DocsApiPaths.openApiContentPath(SYSTEM, COMPONENT);
    private static final String SCHEMA_PATH = DocsApiPaths.databaseSchemaContentPath(SYSTEM, COMPONENT);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemRepository systemRepository;
    @MockitoBean
    private SystemComponentRepository systemComponentRepository;
    @MockitoBean
    private OpenApiSpecRepository openApiSpecRepository;
    @MockitoBean
    private SystemComponentDatabaseSchemaRepository databaseSchemaRepository;

    private SystemComponent component;

    @BeforeEach
    void setUp() {
        System system = System.builder().name(SYSTEM).build();
        component = BackendService.builder().id(UUID.randomUUID()).name(COMPONENT).build();
        system.addSystemComponent(component);

        when(systemRepository.findByNameOrAliasIgnoreCase(SYSTEM)).thenReturn(Optional.of(system));
        when(systemComponentRepository.findByNameIgnoreCase(COMPONENT)).thenReturn(Optional.of(component));
        when(openApiSpecRepository.findByProvider(any())).thenReturn(Optional.empty());
        when(databaseSchemaRepository.findBySystemComponent(any())).thenReturn(Optional.empty());
    }

    @Test
    void getOpenApiSpec_servesTheStoredBytesWithTheContentHashAsEtag() throws Exception {
        when(openApiSpecRepository.findByProvider(component)).thenReturn(Optional.of(openApiSpec()));

        mockMvc.perform(get(OPENAPI_PATH).with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().bytes(SPEC))
                .andExpect(header().string("ETag", "\"sha256:" + ContentHash.of(SPEC) + "\""))
                .andExpect(header().string("Cache-Control", "no-cache"));
    }

    @Test
    void getOpenApiSpec_notModified() throws Exception {
        when(openApiSpecRepository.findByProvider(component)).thenReturn(Optional.of(openApiSpec()));

        mockMvc.perform(get(OPENAPI_PATH)
                        .header("If-None-Match", "\"sha256:" + ContentHash.of(SPEC) + "\"")
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));
    }


    @Test
    void getOpenApiSpec_noSpecPublished() throws Exception {
        mockMvc.perform(get(OPENAPI_PATH).with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type")
                        .value("https://jeap.admin.ch/problems/archrepo/openapi-spec-not-found"));
    }

    @Test
    void getOpenApiSpec_unknownSystem() throws Exception {
        mockMvc.perform(get(DocsApiPaths.openApiContentPath("no-such-system", COMPONENT))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://jeap.admin.ch/problems/archrepo/system-not-found"));
    }

    @Test
    void getOpenApiSpec_unknownComponent() throws Exception {
        mockMvc.perform(get(DocsApiPaths.openApiContentPath(SYSTEM, "no-such-component"))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://jeap.admin.ch/problems/archrepo/component-not-found"));
    }

    @Test
    void getOpenApiSpec_componentOfAnotherSystemIsNotFound() throws Exception {
        // The two path segments must agree: a component that exists but belongs elsewhere is not reachable here
        System otherSystem = System.builder().name("zoll").build();
        SystemComponent foreign = BackendService.builder().id(UUID.randomUUID()).name("foreign").build();
        otherSystem.addSystemComponent(foreign);
        when(systemComponentRepository.findByNameIgnoreCase("foreign")).thenReturn(Optional.of(foreign));

        mockMvc.perform(get(DocsApiPaths.openApiContentPath(SYSTEM, "foreign"))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://jeap.admin.ch/problems/archrepo/component-not-found"));
    }

    @Test
    void getOpenApiSpec_yamlIsNotAcceptable() throws Exception {
        // The API serves one representation, which is what lets the ETag be the hash of the stored bytes
        when(openApiSpecRepository.findByProvider(component)).thenReturn(Optional.of(openApiSpec()));

        mockMvc.perform(get(OPENAPI_PATH).accept(MediaType.parseMediaType("application/yaml"))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void getDatabaseSchema() throws Exception {
        SystemComponentDatabaseSchema schema = databaseSchema();
        when(databaseSchemaRepository.findBySystemComponent(component)).thenReturn(Optional.of(schema));

        mockMvc.perform(get(SCHEMA_PATH).with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"sha256:" + schema.getContentHash() + "\""))
                // The stored blob itself, so the tag names exactly the bytes on the wire
                .andExpect(content().bytes(schema.getSchema()))
                .andExpect(jsonPath("$.tables[0].name").value("declaration"));
    }

    @Test
    void getDatabaseSchema_noSchemaPublished() throws Exception {
        mockMvc.perform(get(SCHEMA_PATH).with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type")
                        .value("https://jeap.admin.ch/problems/archrepo/database-schema-not-found"));
    }


    private OpenApiSpec openApiSpec() {
        return OpenApiSpec.builder()
                .provider(component)
                .version("1.4.2")
                .serverUrl("https://foo-bar.example.com")
                .content(SPEC)
                .build();
    }

    private SystemComponentDatabaseSchema databaseSchema() {
        DatabaseSchema schema = DatabaseSchema.builder()
                .name("wvs_foo_bar")
                .version("42")
                .tables(List.of(Table.builder()
                        .name("declaration")
                        .columns(List.of(TableColumn.builder().name("id").type("uuid").nullable(false).build()))
                        .build()))
                .build();
        return SystemComponentDatabaseSchema.builder()
                .systemComponent(component)
                .schema(schema.toJson())
                .schemaVersion("42")
                .build();
    }

    @Test
    void contentPathsCarryTheContextPath() throws Exception {
        // Resolving contentUrl against the base URL the consumer called must land on the resource again, and the
        // service runs under a context path - a root-relative path would silently drop it.
        when(openApiSpecRepository.findByProvider(component)).thenReturn(Optional.of(openApiSpec()));

        mockMvc.perform(get("/my-archrepo-service" + OPENAPI_PATH).contextPath("/my-archrepo-service")
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk());
        assertThat(DocsApiPaths.openApiContentPath(SYSTEM, COMPONENT))
                .as("outside a request there is no context path to prepend")
                .isEqualTo("/docs-api/systems/wvs/components/wvs-foo-bar-service/openapi");
    }

    @Test
    void aSpecWithoutContentIsAnAbsentSpec() throws Exception {
        // open_api_spec.content is nullable; such a row cannot be served and has no entity tag
        OpenApiSpec withoutContent = OpenApiSpec.builder()
                .provider(component).version("1.4.2").content(null).serverUrl("https://foo-bar.example.com").build();
        when(openApiSpecRepository.findByProvider(component)).thenReturn(Optional.of(withoutContent));

        mockMvc.perform(get(OPENAPI_PATH).with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type")
                        .value("https://jeap.admin.ch/problems/archrepo/openapi-spec-not-found"));
    }

}
