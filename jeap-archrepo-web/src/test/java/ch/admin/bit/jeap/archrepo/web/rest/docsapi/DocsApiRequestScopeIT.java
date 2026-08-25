package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.ContentHash;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.model.database.DatabaseSchema;
import ch.admin.bit.jeap.archrepo.model.database.Table;
import ch.admin.bit.jeap.archrepo.model.database.TableColumn;
import ch.admin.bit.jeap.archrepo.persistence.ArtifactIndexEntry;
import ch.admin.bit.jeap.archrepo.persistence.OpenApiSpecRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentDatabaseSchemaRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.persistence.TeamRepository;
import ch.admin.bit.jeap.archrepo.web.ArchRepoApplication;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The docs API with each request in its own transaction.
 * <p>
 * Deliberately <strong>not</strong> {@code @Transactional}: a test that wraps the request in its own transaction
 * cannot tell whether the handler opened one, and would hide both a lazy-loading failure and a write that is never
 * flushed. {@code DocsApiIT} covers the payloads; this one covers what only a real request scope can show.
 */
@SpringBootTest(classes = ArchRepoApplication.class, properties = "archrepo-config.environment=dev")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(DisableJeapPermitAllSecurityConfiguration.class)
class DocsApiRequestScopeIT extends DocsApiControllerTestBase {

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

    private SystemComponent component;
    private String system;
    private String componentName;

    @BeforeEach
    void seed() {
        // Nothing is rolled back between methods here, so each seeds its own landscape
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        system = "req-scope-system-" + suffix;
        componentName = "req-scope-component-" + suffix;

        Team team = teamRepository.save(Team.builder().name("Team " + UUID.randomUUID()).build());
        component = BackendService.builder()
                .id(UUID.randomUUID()).name(componentName).ownedBy(team).build();
        ReflectionTestUtils.setField(component, "createdAt", ZonedDateTime.now());
        System systemEntity = System.builder()
                .name(system).defaultOwner(team).systemComponents(List.of(component)).build();
        systemRepository.saveAndFlush(systemEntity);
        openApiSpecRepository.saveAndFlush(OpenApiSpec.builder()
                .provider(component).version("1.0.0").content(SPEC).build());
        databaseSchemaRepository.saveAndFlush(SystemComponentDatabaseSchema.builder()
                .systemComponent(component).schema(schemaJson()).schemaVersion("42").build());
    }

    @Test
    void everyResourceAnswersOutsideAnAmbientTransaction() throws Exception {
        // Each of these traverses lazily-loaded associations of the model
        mockMvc.perform(get(DocsApiPaths.SYSTEMS)
                .with(authentication(tokenWithArchitectureModelRead()))).andExpect(status().isOk());
        mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/" + system)
                .with(authentication(tokenWithArchitectureModelRead()))).andExpect(status().isOk());
        mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/" + system + "/messages")
                .with(authentication(tokenWithArchitectureModelRead()))).andExpect(status().isOk());
        mockMvc.perform(get(DocsApiPaths.openApiContentPath(system, componentName))
                .with(authentication(tokenWithArchitectureModelRead()))).andExpect(status().isOk());
        mockMvc.perform(get(DocsApiPaths.databaseSchemaContentPath(system, componentName))
                .with(authentication(tokenWithArchitectureModelRead()))).andExpect(status().isOk());
        mockMvc.perform(get(DocsApiPaths.OPENAPI_SPECS)
                .with(authentication(tokenWithArchitectureModelRead()))).andExpect(status().isOk());
        mockMvc.perform(get(DocsApiPaths.DATABASE_SCHEMAS)
                .with(authentication(tokenWithArchitectureModelRead()))).andExpect(status().isOk());
    }

    @Test
    void readingAnArtifactWritesNothing() throws Exception {
        // The artifact resources are routed to a read replica, so they must not write - the content hash is set
        // when an artifact is stored and, for artifacts predating the column, by the V2_6_0 migration
        ArtifactIndexEntry before = indexEntry();
        String hashBefore = before.getContentHash();
        assertThat(hashBefore).isEqualTo(ContentHash.of(SPEC));

        mockMvc.perform(get(DocsApiPaths.openApiContentPath(system, componentName))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk());

        ArtifactIndexEntry after = indexEntry();
        assertThat(after.getContentHash()).isEqualTo(hashBefore);
        assertThat(after.getLastModifiedAt())
                .as("a read must not make an artifact look freshly published")
                .isEqualTo(before.getLastModifiedAt());
    }

    private ArtifactIndexEntry indexEntry() {
        return openApiSpecRepository.findIndexEntriesBySystemName(system).getFirst();
    }

    private static byte[] schemaJson() {
        return DatabaseSchema.builder().name("s").version("42")
                .tables(List.of(Table.builder().name("t")
                        .columns(List.of(TableColumn.builder().name("id").type("uuid").nullable(false).build()))
                        .build()))
                .build().toJson();
    }
}
