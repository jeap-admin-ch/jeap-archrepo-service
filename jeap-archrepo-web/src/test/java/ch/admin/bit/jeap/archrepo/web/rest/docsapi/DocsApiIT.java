package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageVersion;
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
    private static final String EVENT_NAME = "WvsDeclarationAcceptedEvent";
    private static final String COMMAND_NAME = "WvsCheckNctsReferabilityV2Command";
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
                .andExpect(jsonPath(seededSystem(), hasSize(1)))
                .andExpect(jsonPath(seededSystem() + ".name").value(SYSTEM))
                .andExpect(jsonPath(seededSystem() + ".aliases[0]").value("WVS-ALIAS"))
                .andExpect(jsonPath(seededSystem() + ".team.name").value("Team Blue"));
    }

    /**
     * The seeded system within the answer, rather than {@code $.systems[0]} and a total count.
     * <p>
     * This class is {@code @Transactional} and rolls its own writes back, but it reads the same database as the
     * integration tests that are deliberately <b>not</b> - {@code DocsApiRequestScopeIT} and
     * {@code ImportersOverHttpIT} both have to commit, because a request has to find the landscape in the
     * database rather than in a persistence context shared with the test. What they commit stays there, so
     * whether this class sees only its own system depends on the order Surefire happens to run the classes in.
     * It once passed locally and failed on the build server for exactly that reason.
     */
    private static String seededSystem() {
        return "$.systems[?(@.name == '%s')]".formatted(SYSTEM);
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
        // Scoped to the seeded system for the reason given on seededSystem(): the index lists every artifact of
        // every system, including those a non-transactional test class committed before this one ran
        String seededArtifact = "$.artifacts[?(@.system == '%s')]".formatted(SYSTEM);

        String indexBody = mockMvc.perform(get(indexPath)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath(seededArtifact, hasSize(1)))
                .andReturn().getResponse().getContentAsString();

        // A filter expression is an indefinite path, so JsonPath answers with the matches rather than the value
        String etagInIndex = JsonPath.<List<String>>read(indexBody, seededArtifact + ".etag").getFirst();
        String contentUrl = JsonPath.<List<String>>read(indexBody, seededArtifact + ".contentUrl").getFirst();

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

    @Test
    void getMessageTypes_listsEveryVersionOfEveryMessageType() throws Exception {
        seedMessageTypes();

        mockMvc.perform(get(DocsApiPaths.MESSAGE_TYPES)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.messageTypes", hasSize(2)))
                // Sorted by system, message type and version, across the two entities that hold them
                .andExpect(jsonPath("$.messageTypes[0].message").value(COMMAND_NAME))
                .andExpect(jsonPath("$.messageTypes[0].kind").value("COMMAND"))
                .andExpect(jsonPath("$.messageTypes[1].message").value(EVENT_NAME))
                .andExpect(jsonPath("$.messageTypes[1].kind").value("EVENT"))
                .andExpect(jsonPath("$.messageTypes[1].versions", hasSize(3)))
                // Ordered as numbers: as text, 10.0.0 would stand between 1.0.0 and 2.0.0
                .andExpect(jsonPath("$.messageTypes[1].versions[0].version").value("1.0.0"))
                .andExpect(jsonPath("$.messageTypes[1].versions[1].version").value("2.0.0"))
                .andExpect(jsonPath("$.messageTypes[1].versions[2].version").value("10.0.0"));
    }

    @Test
    void getMessageTypes_filteredBySystemResolvesAnAlias() throws Exception {
        seedMessageTypes();

        mockMvc.perform(get(DocsApiPaths.MESSAGE_TYPES).param("system", "WVS-ALIAS")
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageTypes", hasSize(2)));
    }

    @Test
    void getMessageTypeVersion_namesTheMessageTypeAsItIsStored() throws Exception {
        seedMessageTypes();

        // Addressed in the wrong case, answered with the stored spelling - as the system name is
        mockMvc.perform(get(DocsApiPaths.messageTypeVersionPath("WVS-ALIAS", EVENT_NAME.toLowerCase(), "2.0.0"))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.system").value(SYSTEM))
                .andExpect(jsonPath("$.message").value(EVENT_NAME));
    }

    @Test
    void getMessageTypeVersion_servesTheStoredSchemas() throws Exception {
        seedMessageTypes();

        mockMvc.perform(get(DocsApiPaths.messageTypeVersionPath(SYSTEM, EVENT_NAME, "2.0.0"))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2.0.0"))
                .andExpect(jsonPath("$.compatibilityMode").value("BACKWARD"))
                .andExpect(jsonPath("$.compatibleVersion").value("1.0.0"))
                .andExpect(jsonPath("$.key.schemaName").value("Key-2.0.0.avdl"))
                .andExpect(jsonPath("$.value.resolvedSchema").value("value schema 2.0.0"));
    }

    /**
     * That the index is walkable: every version it lists can be fetched at the path it carries, context path
     * and encoding included.
     */
    @Test
    void everyVersionInTheIndexCanBeFetched() throws Exception {
        seedMessageTypes();

        String index = mockMvc.perform(get(DocsApiPaths.MESSAGE_TYPES)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> contentUrls = JsonPath.read(index, "$.messageTypes[*].versions[*].contentUrl");
        assertThat(contentUrls).hasSize(4);
        for (String contentUrl : contentUrls) {
            mockMvc.perform(get(contentUrl).with(authentication(tokenWithArchitectureModelRead())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.value.resolvedSchema").exists());
        }
    }

    /**
     * The message types are seeded per test rather than in {@link #seedLandscape()}: the resources above assert
     * a landscape without messages, and this test class writes inside its own transaction anyway.
     */
    private void seedMessageTypes() {
        System system = systemRepository.findByNameIgnoreCase(SYSTEM).orElseThrow();
        system.addEvent(Event.builder()
                .id(UUID.randomUUID())
                .messageTypeName(EVENT_NAME)
                .scope(SYSTEM)
                .topic("wvs-declaration-event")
                .descriptorUrl("https://registry.example.com/declaration.json")
                .messageVersions(List.of(messageVersion("1.0.0", null), messageVersion("2.0.0", "1.0.0"),
                        messageVersion("10.0.0", "2.0.0")))
                .build());
        system.addCommand(Command.builder()
                .id(UUID.randomUUID())
                .messageTypeName(COMMAND_NAME)
                .scope(SYSTEM)
                .topic("wvs-ncts-command")
                .descriptorUrl("https://registry.example.com/ncts.json")
                .messageVersions(List.of(messageVersion("1.0.0", null)))
                .build());
        systemRepository.saveAndFlush(system);
        entityManager.flush();
        entityManager.clear();
    }

    private static MessageVersion messageVersion(String version, String compatibleVersion) {
        return MessageVersion.builder()
                .version(version)
                .keySchemaName("Key-" + version + ".avdl")
                .keySchemaUrl("https://registry.example.com/Key-" + version + ".avdl")
                .keySchemaResolved("key schema " + version)
                .valueSchemaName("Value-" + version + ".avdl")
                .valueSchemaUrl("https://registry.example.com/Value-" + version + ".avdl")
                .valueSchemaResolved("value schema " + version)
                .compatibilityMode(compatibleVersion == null ? null : "BACKWARD")
                .compatibleVersion(compatibleVersion)
                .build();
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
