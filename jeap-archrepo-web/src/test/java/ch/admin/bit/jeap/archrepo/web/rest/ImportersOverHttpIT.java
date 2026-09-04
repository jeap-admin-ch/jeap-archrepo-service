package ch.admin.bit.jeap.archrepo.web.rest;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.OpenApiSpecRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.persistence.TeamRepository;
import ch.admin.bit.jeap.archrepo.web.ArchRepoApplication;
import ch.admin.bit.jeap.archrepo.web.rest.docsapi.DocsApiPaths;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import ch.admin.bit.jeap.archrepo.web.PostgresIntegrationTestBase;

/**
 * Every endpoint that reads the lazily loaded {@code importers} of a relation or a REST API, over HTTP, with
 * each request in its own transaction.
 * <p>
 * <b>Deliberately not {@code @Transactional}.</b> A test that wraps the request in its own transaction keeps a
 * session open around the handler and would pass whether or not the handler opened one - which is exactly the
 * failure a lazy association introduces. This application runs with {@code spring.jpa.open-in-view=false}, so
 * there is no session-per-request either: a handler that traverses {@code importers} without a transaction of
 * its own throws {@code LazyInitializationException}, and answers 500. That is what these tests would catch.
 * <p>
 * The assertions go past the status code on purpose. A collection that silently came back empty would still
 * answer 200, and the endpoint below that filters on {@code importers} would then answer with an empty list
 * rather than an error - so each test asserts the payload that only a loaded collection can produce.
 */
@SpringBootTest(classes = ArchRepoApplication.class, properties = "archrepo-config.environment=dev")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(DisableJeapPermitAllSecurityConfiguration.class)
// PER_CLASS so that the landscape is committed once rather than once per test method - see seed method
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImportersOverHttpIT extends PostgresIntegrationTestBase {

    /** Matches {@code jeap.security.oauth2.resourceserver.system-name} in {@code application-test.yml}. */
    private static final String SYSTEM_NAME = "application-platform";

    /** The value {@code archrepoDefaultProperties.properties} ships. */
    private static final int EXPECTED_BATCH_FETCH_SIZE = 100;

    private static final byte[] SPEC = "{\"openapi\":\"3.0.0\"}".getBytes(StandardCharsets.UTF_8);
    private static final String EVENT_NAME = "WvsDeclarationAcceptedEvent";
    private static final String REST_PATH = "/api/declarations/{id}";
    private static final String PACT_URL = "https://pactbroker.example.com/pacts/declarations";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SystemRepository systemRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private OpenApiSpecRepository openApiSpecRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private String system;
    private String provider;
    private String consumer;

    /**
     * Seeded <b>once for the class</b>, not per method. Nothing here is rolled back - the requests must find the
     * landscape in the database rather than in a persistence context this test is sharing with them - and every
     * test below only reads, so seeding per method would leave one committed landscape per test method behind
     * for the rest of the suite. That is what broke {@code DocsApiIT} on a build whose run order put this class
     * first: it seeded seven.
     * <p>
     * The names still carry a random suffix, because what is committed here outlives the class.
     */
    @BeforeAll
    void seedALandscapeWithRelationsAndRestApis() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        system = "importers-system-" + suffix;
        provider = "importers-provider-" + suffix;
        consumer = "importers-consumer-" + suffix;

        Team team = teamRepository.save(Team.builder().name("Team " + suffix).build());
        SystemComponent providerComponent = component(provider, team);
        SystemComponent consumerComponent = component(consumer, team);

        System systemEntity = System.builder()
                .name(system)
                .defaultOwner(team)
                .systemComponents(List.of(providerComponent, consumerComponent))
                .build();

        // One REST API imported from an OpenAPI spec, which is what /api/openapi/{component}/rest-apis filters
        // on, and one relation onto it. Plus an event relation, so the docs API has a relation of each kind.
        RestApi restApi = RestApi.builder()
                .provider(providerComponent).method("GET").path(REST_PATH)
                .importer(Importer.OPEN_API)
                .build();
        systemEntity.addRestApi(restApi);
        systemEntity.addRelation(RestApiRelation.builder()
                .definingSystem(systemEntity)
                .consumerName(consumer)
                .restApi(restApi)
                .pactUrl(PACT_URL)
                .lastSeen(ZonedDateTime.now())
                .importer(Importer.PACT_BROKER)
                .build());
        systemEntity.addRelation(EventRelation.builder()
                .definingSystem(systemEntity)
                .providerName(provider)
                .consumerName(consumer)
                .eventName(EVENT_NAME)
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .build());

        systemRepository.saveAndFlush(systemEntity);
        openApiSpecRepository.saveAndFlush(OpenApiSpec.builder()
                .provider(providerComponent).version("1.4.2")
                .serverUrl("https://declarations.example.com").content(SPEC)
                .build());
    }

    /**
     * The endpoint the doc service reads the model with, and the one whose cost the lazy mapping addresses:
     * {@code createRelations} walks the active relations of every system to answer for one of them.
     */
    @Test
    void docsApiSystemDetailServesItsRelations() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/" + system)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relations", hasSize(2)))
                .andExpect(jsonPath("$.relations[?(@.type == 'EVENT_RELATION')].messageType")
                        .value(EVENT_NAME))
                .andExpect(jsonPath("$.relations[?(@.type == 'REST_API_RELATION')].path")
                        .value(REST_PATH))
                .andExpect(jsonPath("$.relations[?(@.type == 'REST_API_RELATION')].pactUrl")
                        .value(PACT_URL));
    }

    @Test
    void docsApiSystemListAnswers() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk());
    }

    /**
     * The one endpoint that reads {@code importers} into its answer rather than only traversing the graph that
     * holds them: it keeps the REST APIs imported from an OpenAPI spec and drops the rest. An {@code importers}
     * set that came back empty would leave this list empty, so the payload is the assertion.
     */
    @Test
    void openApiRestApisEndpointFiltersOnTheImportersOfEachRestApi() throws Exception {
        mockMvc.perform(get("/api/openapi/" + provider + "/rest-apis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.4.2"))
                .andExpect(jsonPath("$.restApis", hasSize(1)))
                .andExpect(jsonPath("$.restApis[0].method").value("GET"))
                .andExpect(jsonPath("$.restApis[0].path").value(REST_PATH));
    }

    /** The model API's own relation view, which walks the same associations as the docs API's. */
    @Test
    void modelApiRelationsEndpointAnswers() throws Exception {
        mockMvc.perform(get("/api/model/" + system + "/relations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    /** Walks every REST API relation of every system and reads the REST API behind each. */
    @Test
    void modelApiRestApiRelationsWithoutPactEndpointAnswers() throws Exception {
        mockMvc.perform(get("/api/model/rest-api-relation-without-pact"))
                .andExpect(status().isOk());
    }

    @Test
    void modelApiEndpointAnswers() throws Exception {
        mockMvc.perform(get("/api/model"))
                .andExpect(status().isOk());
    }

    /**
     * That the batch size is not only written down but arrives.
     * <p>
     * It is set in {@code archrepoDefaultProperties.properties} via a {@code @PropertySource}, which is a lower
     * precedence source than {@code application-test.yml} - and that file sets another key under the same
     * {@code spring.jpa.properties.hibernate} prefix. The keys of a map are merged across property sources
     * rather than the whole map being replaced, so both arrive; this asserts it, because if that ever stopped
     * holding, every read of the model would quietly go back to one query per row.
     */
    @Test
    void theShippedBatchFetchSizeReachesHibernate() {
        // The SPI rather than SessionFactory.getSessionFactoryOptions(), which is deprecated. Either way this
        // is the value the session factory was built with, not the string that was configured.
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);

        assertThat(sessionFactory.getSessionFactoryOptions().getDefaultBatchFetchSize())
                .as("spring.jpa.properties.hibernate.default_batch_fetch_size from "
                    + "archrepoDefaultProperties.properties must reach the session factory")
                .isEqualTo(EXPECTED_BATCH_FETCH_SIZE);
    }

    private static SystemComponent component(String name, Team team) {
        SystemComponent component = BackendService.builder()
                .id(UUID.randomUUID()).name(name).ownedBy(team).build();
        ReflectionTestUtils.setField(component, "createdAt", ZonedDateTime.now());
        return component;
    }

    private static JeapAuthenticationToken tokenWithArchitectureModelRead() {
        return JeapAuthenticationTestTokenBuilder.create()
                .withUserRoles(SemanticApplicationRole.builder()
                        .system(SYSTEM_NAME)
                        .resource("architecture-model")
                        .operation("read")
                        .build())
                .build();
    }
}
