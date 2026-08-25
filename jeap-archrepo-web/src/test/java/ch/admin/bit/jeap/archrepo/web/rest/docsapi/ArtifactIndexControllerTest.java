package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.persistence.ArtifactIndexEntry;
import ch.admin.bit.jeap.archrepo.persistence.OpenApiSpecRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentDatabaseSchemaRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ArtifactIndexController.class,
        properties = "archrepo.openapi-base-url=https://archrepo.example.com/archrepo-service/api/openapi/")
@Import({DocsApiTestConfiguration.class, WebSecurityConfig.class,
        MvcSecurityConfiguration.class, ResourceServerProperties.class, TokenConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArtifactIndexControllerTest extends DocsApiControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemRepository systemRepository;
    @MockitoBean
    private OpenApiSpecRepository openApiSpecRepository;
    @MockitoBean
    private SystemComponentDatabaseSchemaRepository databaseSchemaRepository;

    @BeforeEach
    void setUp() {
        when(openApiSpecRepository.findIndexEntries()).thenReturn(List.of(entry("wvs", "wvs-foo-bar-service", "1.4.2", "41ab7c")));
        when(openApiSpecRepository.findIndexEntriesBySystemName("wvs"))
                .thenReturn(List.of(entry("wvs", "wvs-foo-bar-service", "1.4.2", "41ab7c")));
        when(openApiSpecRepository.findIndexEntriesBySystemName("zoll")).thenReturn(List.of());
        when(databaseSchemaRepository.findIndexEntries()).thenReturn(List.of(entry("wvs", "wvs-foo-bar-service", "42", "77de10")));
        when(systemRepository.findByNameOrAliasIgnoreCase("wvs"))
                .thenReturn(Optional.of(ch.admin.bit.jeap.archrepo.metamodel.System.builder().name("wvs").build()));
        when(systemRepository.findByNameOrAliasIgnoreCase("WVS-ALIAS"))
                .thenReturn(Optional.of(ch.admin.bit.jeap.archrepo.metamodel.System.builder().name("wvs").build()));
        when(systemRepository.findByNameOrAliasIgnoreCase("no-such-system")).thenReturn(Optional.empty());
    }

    @Test
    void getOpenApiSpecs() throws Exception {
        mockMvc.perform(get(DocsApiPaths.OPENAPI_SPECS).with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.artifacts", hasSize(1)))
                .andExpect(jsonPath("$.artifacts[0].system").value("wvs"))
                .andExpect(jsonPath("$.artifacts[0].component").value("wvs-foo-bar-service"))
                .andExpect(jsonPath("$.artifacts[0].version").value("1.4.2"))
                .andExpect(jsonPath("$.artifacts[0].etag").value("\"sha256:41ab7c\""))
                .andExpect(jsonPath("$.artifacts[0].contentUrl")
                        .value("/docs-api/systems/wvs/components/wvs-foo-bar-service/openapi"));
    }

    @Test
    void getOpenApiSpecs_filteredBySystem() throws Exception {
        mockMvc.perform(get(DocsApiPaths.OPENAPI_SPECS).param("system", "wvs")
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifacts", hasSize(1)));
    }

    @Test
    void getOpenApiSpecs_theFilterResolvesAnAlias() throws Exception {
        // Every other {system} in the API resolves aliases; matching the stored name alone would answer an alias
        // with an empty index and a consumer would silently replicate nothing
        mockMvc.perform(get(DocsApiPaths.OPENAPI_SPECS).param("system", "WVS-ALIAS")
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifacts", hasSize(1)));
    }

    @Test
    void getOpenApiSpecs_anUnknownFilterIsNotFound() throws Exception {
        mockMvc.perform(get(DocsApiPaths.OPENAPI_SPECS).param("system", "no-such-system")
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://jeap.admin.ch/problems/archrepo/system-not-found"));
    }

    @Test
    void getOpenApiSpecs_notModified() throws Exception {
        String etag = mockMvc.perform(get(DocsApiPaths.OPENAPI_SPECS)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andReturn().getResponse().getHeader("ETag");

        mockMvc.perform(get(DocsApiPaths.OPENAPI_SPECS).header("If-None-Match", etag)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotModified());
    }

    @Test
    void getDatabaseSchemas() throws Exception {
        mockMvc.perform(get(DocsApiPaths.DATABASE_SCHEMAS).with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifacts[0].etag").value("\"sha256:77de10\""))
                .andExpect(jsonPath("$.artifacts[0].version").value("42"))
                .andExpect(jsonPath("$.artifacts[0].contentUrl")
                        .value("/docs-api/systems/wvs/components/wvs-foo-bar-service/database-schema"));
    }

    @Test
    void anEntryWithoutAStoredHashHasNoEtag() throws Exception {
        when(openApiSpecRepository.findIndexEntries())
                .thenReturn(List.of(entry("wvs", "wvs-foo-bar-service", "1.4.2", null)));

        mockMvc.perform(get(DocsApiPaths.OPENAPI_SPECS).with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifacts[0].etag").doesNotExist());
    }

    private static ArtifactIndexEntry entry(String system, String component, String version, String contentHash) {
        return new ArtifactIndexEntry() {
            @Override
            public String getSystem() {
                return system;
            }

            @Override
            public String getComponent() {
                return component;
            }

            @Override
            public String getVersion() {
                return version;
            }

            @Override
            public String getContentHash() {
                return contentHash;
            }

            @Override
            public ZonedDateTime getLastModifiedAt() {
                return ZonedDateTime.parse("2026-08-12T05:31:00Z");
            }
        };
    }
}
