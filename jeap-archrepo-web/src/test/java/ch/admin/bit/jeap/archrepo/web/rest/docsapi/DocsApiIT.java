package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.model.database.DatabaseSchema;
import ch.admin.bit.jeap.archrepo.model.database.Table;
import ch.admin.bit.jeap.archrepo.model.database.TableColumn;
import ch.admin.bit.jeap.archrepo.persistence.OpenApiSpecRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentDatabaseSchemaRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.persistence.TeamRepository;
import ch.admin.bit.jeap.archrepo.web.ArchRepoApplication;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The docs API against a migrated database with a seeded landscape - the level at which the migration, the lazy
 * hash backfill and the index-to-content ETag identity are provable at all.
 */
@SpringBootTest(classes = ArchRepoApplication.class, properties = "archrepo-config.environment=dev")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(DisableJeapPermitAllSecurityConfiguration.class)
@Transactional
class DocsApiIT extends DocsApiControllerTestBase {

    private static final String SYSTEM = "wvs";
    private static final String COMPONENT = "wvs-foo-bar-service";
    private static final byte[] SPEC = "{\"openapi\":\"3.0.0\"}".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SystemRepository systemRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private OpenApiSpecRepository openApiSpecRepository;
    @Autowired
    private SystemComponentDatabaseSchemaRepository databaseSchemaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private SystemComponent component;

    @BeforeEach
    void seedLandscape() {
        Team team = teamRepository.save(Team.builder()
                .name("Team Blue").contactAddress("team-blue@example.com").build());

        component = BackendService.builder().id(UUID.randomUUID()).name(COMPONENT).ownedBy(team).build();
        ReflectionTestUtils.setField(component, "createdAt", ZonedDateTime.now());

        System system = System.builder()
                .name(SYSTEM)
                .description("Warenverkehrssystem")
                .aliases(List.of("WVS-ALIAS"))
                .defaultOwner(team)
                .systemComponents(List.of(component))
                .build();
        systemRepository.saveAndFlush(system);

        openApiSpecRepository.saveAndFlush(OpenApiSpec.builder()
                .provider(component).version("1.4.2").serverUrl("https://foo-bar.example.com").content(SPEC).build());
        databaseSchemaRepository.saveAndFlush(SystemComponentDatabaseSchema.builder()
                .systemComponent(component).schema(databaseSchema()).schemaVersion("42").build());

        // The artifacts are saved through their own repositories, as the push endpoints do. Clear the persistence
        // context so the API re-reads the aggregate instead of seeing the System with its collections as they
        // were before - in production every request is its own transaction and reads them fresh anyway.
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void getSystems() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS).with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.systems", hasSize(1)))
                .andExpect(jsonPath("$.systems[0].name").value(SYSTEM))
                .andExpect(jsonPath("$.systems[0].aliases[0]").value("WVS-ALIAS"))
                .andExpect(jsonPath("$.systems[0].team.name").value("Team Blue"));
    }

    @Test
    void getSystem_withItsComponentAndArtifactPointers() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/" + SYSTEM)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components", hasSize(1)))
                .andExpect(jsonPath("$.components[0].name").value(COMPONENT))
                .andExpect(jsonPath("$.components[0].openApi.version").value("1.4.2"))
                .andExpect(jsonPath("$.components[0].openApi.contentUrl")
                        .value(DocsApiPaths.openApiContentPath(SYSTEM, COMPONENT)))
                .andExpect(jsonPath("$.components[0].databaseSchema.schemaVersion").value("42"));
    }

    @Test
    void getMessages_isEmptyForASystemWithoutMessages() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/" + SYSTEM + "/messages")
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(0)));
    }

    @Test
    void getOpenApiSpec_servesTheStoredBytes() throws Exception {
        mockMvc.perform(get(DocsApiPaths.openApiContentPath(SYSTEM, COMPONENT))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(content().string(new String(SPEC, StandardCharsets.UTF_8)));
    }

    @Test
    void getDatabaseSchema_servesTheStoredSchema() throws Exception {
        mockMvc.perform(get(DocsApiPaths.databaseSchemaContentPath(SYSTEM, COMPONENT))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tables[0].name").value("declaration"));
    }

    /**
     * The contract that makes the replication indexes useful: the tag in the index is byte-identical to the tag
     * of the resource it points at, so a consumer can decide without a request - and the tag it stored answers
     * 304 when it does ask.
     */
    @Test
    void theIndexEtagIsTheContentResourceEtag() throws Exception {
        assertIndexEtagMatchesContent(DocsApiPaths.OPENAPI_SPECS);
        assertIndexEtagMatchesContent(DocsApiPaths.DATABASE_SCHEMAS);
    }

    private void assertIndexEtagMatchesContent(String indexPath) throws Exception {
        String indexBody = mockMvc.perform(get(indexPath)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artifacts", hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        String etagInIndex = JsonPath.read(indexBody, "$.artifacts[0].etag");
        String contentUrl = JsonPath.read(indexBody, "$.artifacts[0].contentUrl");

        String etagOfContent = mockMvc.perform(get(contentUrl)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("ETag");

        assertThat(etagInIndex)
                .as("the etag in %s must be byte-identical to the ETag header of %s", indexPath, contentUrl)
                .isEqualTo(etagOfContent);

        mockMvc.perform(get(contentUrl).header("If-None-Match", etagOfContent)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotModified());
    }

    // The backfill of a hash that predates the column is proven in DocsApiRequestScopeIT: it is written with a
    // bulk update that bypasses the persistence context, so it cannot be observed from inside this test's own
    // transaction.

    private static byte[] databaseSchema() {
        return DatabaseSchema.builder()
                .name("wvs_foo_bar")
                .version("42")
                .tables(List.of(Table.builder()
                        .name("declaration")
                        .columns(List.of(TableColumn.builder().name("id").type("uuid").nullable(false).build()))
                        .build()))
                .build()
                .toJson();
    }
}
