package ch.admin.bit.jeap.archrepo.web.rest.model;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.AllowOverridePactUrl;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.model.database.*;
import ch.admin.bit.jeap.archrepo.persistence.*;
import ch.admin.bit.jeap.archrepo.test.DocsApiPactStubs;
import ch.admin.bit.jeap.archrepo.web.ArchRepoApplication;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.ServletSemanticAuthorization;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilderFactory;
import ch.admin.bit.jeap.security.test.resource.configuration.JeapOAuth2IntegrationTestResourceConfiguration;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static ch.admin.bit.jeap.archrepo.test.Pacticipants.ARCHREPO;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

@SuppressWarnings("unused")
// A defined port rather than a random one: the docs API is bearer-authenticated, and the tokens the
// verification signs are validated against the JWKS this application serves itself under the test profile - so
// the port has to be known before the context starts. Both it and the JWKS URI are in
// application-pact-provider-test.yml.
@SpringBootTest(classes = ArchRepoApplication.class, webEnvironment = DEFINED_PORT)
@ActiveProfiles({"pact-provider-test"})
// Serves that JWKS, and switches off the permit-all chain of jeap-spring-boot-security-starter-test - without
// which production security would be shadowed and every interaction of the docs API would be verified against
// an unprotected API.
@Import(JeapOAuth2IntegrationTestResourceConfiguration.class)
@Provider(ARCHREPO)
@PactBroker
@IgnoreNoPactsToVerify
@AllowOverridePactUrl
public class PactProviderTestBase {
    /**
     * The image of the PostgreSQL the verification runs against, overridable for a build that pulls its images
     * from a mirror rather than from Docker Hub.
     */
    public static final String POSTGRES_IMAGE_PROPERTY = "archrepo.test.postgres-image";

    /**
     * A real PostgreSQL, in the version the service runs on in production.
     * <p>
     * Every repository the verified endpoints use is mocked, but the application context is the real one and
     * does not start without the JPA stack, so a database has to be there. <b>Running this verification needs a
     * Docker daemon.</b>
     * <p>
     * Started once for the whole test JVM and never stopped: the Spring context outlives any per-class
     * lifecycle.
     */
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse(
            java.lang.System.getProperty(POSTGRES_IMAGE_PROPERTY, "postgres:17-alpine")));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final String BEARER = "Bearer ";

    /**
     * The resource of the docs API's semantic role. Every one of its resources requires
     * {@code <system-name>_@architecture-model_#read} and there is no second role in it, so one token opens all
     * of them.
     */
    private static final String ARCHITECTURE_MODEL = "architecture-model";

    private static final String TEST_SYSTEM = "test-system";
    private static final String TEST_COMPONENT = "test-component";
    private static final String TEST_VERSION = "1.2.3";
    private static final String COLUMN_A = "column_a";

    @LocalServerPort
    private int localServerPort;

    @MockitoBean
    ArchitectureModelRepository architectureModelRepository;

    @MockitoBean
    OpenApiSpecRepository openApiSpecRepository;

    @MockitoBean
    SystemRepository systemRepository;

    @MockitoBean
    SystemComponentDatabaseSchemaRepository systemComponentDatabaseSchemaRepository;

    @MockitoBean
    SystemComponentRepository systemComponentRepository;

    @MockitoBean
    ComponentGraphRepository componentGraphRepository;

    @MockitoBean
    RestApiRepository restApiRepository;

    @MockitoBean
    MessageTypeVersionRepository messageTypeVersionRepository;

    @MockitoBean
    ServletSemanticAuthorization semanticAuthorization;

    /**
     * Signs the token every request is verified with. The key it signs with is the one the JWKS endpoint of
     * {@link JeapOAuth2IntegrationTestResourceConfiguration} publishes, which is what makes the token valid for
     * this application and for no other.
     */
    @Autowired(required = false)
    private JwsBuilderFactory jwsBuilderFactory;

    /**
     * The system the semantic roles of this instance are named after - the first part of every role, and
     * checked, so a token carrying another landscape's role is rejected. Read from the configuration rather
     * than spelled here, so that the token stays valid for whatever an instance's profile sets.
     */
    // Fully qualified: lombok.Value is imported here, and the two would collide
    @org.springframework.beans.factory.annotation.Value("${jeap.security.oauth2.resourceserver.system-name}")
    private String securitySystemName;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        // If there are no pacts there will be no context.
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", localServerPort, "/"));
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testPacts(PactVerificationContext context, HttpRequest request) {
        // If there are no pacts there will be no context, and no request either.
        if (context != null) {
            if (request != null) {
                authenticate(request);
            }
            context.verifyInteraction();
        }
    }

    /**
     * Replaces whatever bearer token a pact recorded with one signed here and now.
     * <p>
     * <b>A token cannot be part of a contract.</b> One written into a pact file when the consumer's test ran
     * would be replayed by this verification days or weeks later, long expired, and every interaction of the
     * docs API would fail on the clock rather than on the contract. So a consumer records a placeholder, this
     * puts a fresh token in its place, and <b>no interaction may assert anything about the
     * {@code Authorization} header</b> - injecting a request header is the mechanism pact-jvm offers for
     * exactly this, and its price is that the header is no longer what the consumer wrote.
     * <p>
     * The token carries the semantic role of the docs API, so <b>authorization is exercised rather than
     * mocked</b>: that API is authorized with {@code @PreAuthorize}, which jEAP evaluates against the roles in
     * the token and not against the {@link ServletSemanticAuthorization} bean this class mocks. That bean is
     * what the OpenAPI upload consults programmatically, which is why its own states stub it.
     * <p>
     * <b>Only a bearer header is replaced.</b> A request carrying none belongs to an interaction about being
     * unauthenticated, and one carrying HTTP basic credentials belongs to the {@code /api} chain, which
     * authenticates against {@code archrepo.api.secret} - substituting a token into either would verify
     * something the consumer never sends.
     */
    private void authenticate(HttpRequest request) {
        if (jwsBuilderFactory == null || !isBearer(request)) {
            return;
        }
        String token = jwsBuilderFactory
                .createValidFromNowBuilder("doc-service", JeapAuthenticationContext.SYS, 5, ChronoUnit.MINUTES)
                .withUserRoles(SemanticApplicationRole.builder()
                        .system(securitySystemName)
                        .resource(ARCHITECTURE_MODEL)
                        .operation("read")
                        .build())
                .build()
                .serialize();
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER + token);
    }

    private static boolean isBearer(HttpRequest request) {
        Header authorization = request.getFirstHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null && authorization.getValue() != null
               && authorization.getValue().startsWith(BEARER);
    }

    @SneakyThrows
    @State("A model with one system and one service")
    void simpleModel() {
        ArchitectureModel architectureModel = ModelStub.createSimpleModel();
        when(architectureModelRepository.load()).thenReturn(architectureModel);
    }

    @SneakyThrows
    @State("A model with one rest api relation")
    void restApiRelations() {
        ArchitectureModel architectureModel = ModelStub.createSimpleModelWithOneRestApiRelation();
        when(architectureModelRepository.load()).thenReturn(architectureModel);
    }

    @State("A model with one component with an OpenAPI documentation")
    void openApiDocumentationVersions() {
        when(openApiSpecRepository.getApiDocVersions()).thenReturn(
                List.of(new ApiDocVersionImpl(TEST_SYSTEM, TEST_COMPONENT, TEST_VERSION)));
    }

    @State("A model with one component with a database schema")
    void databaseSchemaVersions() {
        when(systemComponentDatabaseSchemaRepository.getDatabaseSchemaVersions()).thenReturn(
                List.of(new DatabaseSchemaVersionImpl(TEST_SYSTEM, TEST_COMPONENT, TEST_VERSION)));
    }

    @State("A database schema exists for the component 'test-component' in the system 'test-system'")
    @SneakyThrows
    void databaseSchemaExists() {
        SystemComponent systemComponent = mockSystemAndComponent(TEST_SYSTEM, TEST_COMPONENT);
        DatabaseSchema databaseSchema = getFullDatabaseSchema();
        SystemComponentDatabaseSchema systemComponentDatabaseSchema = SystemComponentDatabaseSchema.builder().
                systemComponent(systemComponent)
                .schema(databaseSchema.toJson())
                .schemaVersion(databaseSchema.version())
                .build();
        when(systemComponentDatabaseSchemaRepository.findBySystemComponent(systemComponent)).
                thenReturn(Optional.of(systemComponentDatabaseSchema));
    }

    @State("No database schema exists for the component 'test-component' in the system 'test-system'")
    void noDatabaseSchemaExists() {
        SystemComponent systemComponent = mockSystemAndComponent(TEST_SYSTEM, TEST_COMPONENT);
        when(systemComponentDatabaseSchemaRepository.findBySystemComponent(systemComponent)).
                thenReturn(Optional.empty());
    }

    @State("A REST API documentation for the component 'test-component' in the system 'test-system' exists")
    void restApiDocumentationExists() {
        SystemComponent systemComponent = mockSystemAndComponent(TEST_SYSTEM, TEST_COMPONENT);
        when(systemComponentRepository.findByNameIgnoreCase(TEST_COMPONENT)).thenReturn(Optional.of(systemComponent));

        ApiDocDto apiDocDto = mock(ApiDocDto.class);
        when(apiDocDto.getServerUrl()).thenReturn("https://api.example.com");
        when(apiDocDto.getVersion()).thenReturn("1.0.0");
        when(apiDocDto.getCreatedAt()).thenReturn(ZonedDateTime.of(2025, 8, 1, 14, 0, 0, 0, ZoneId.of("UTC")));
        when(apiDocDto.getModifiedAt()).thenReturn(ZonedDateTime.of(2025, 8, 2, 12, 0, 0, 0, ZoneId.of("UTC")));
        when(openApiSpecRepository.getApiDocVersion(systemComponent)).thenReturn(Optional.of(apiDocDto));

        RestApi restApi = mock(RestApi.class);
        when(restApi.getMethod()).thenReturn("GET");
        when(restApi.getPath()).thenReturn("/api/foo");
        when(restApi.getImporters()).thenReturn(Set.of(Importer.OPEN_API));
        when(restApiRepository.findByProvider(systemComponent)).thenReturn(List.of(restApi));
    }

    @State("User is authorized for OpenAPI document upload")
    void userIsAuthorizedForOpenApiDocUpload() {
        when(semanticAuthorization.hasRole("openapidoc", "write")).thenReturn(true);
    }

    @State("User is not authorized for OpenAPI document upload")
    void userIsNotAuthorizedForOpenApiDocUpload() {
        when(semanticAuthorization.hasRole("openapidoc", "write")).thenReturn(false);
    }

    // --- The docs API -------------------------------------------------------------------------------------
    //
    // Five states for the nine resources of /docs-api, and the mapping from route to state is
    // DOCS_API_ROUTE_STATES below - which DocsApiPactStateCoverageTest holds against the routes the
    // controllers actually declare, so that a tenth resource cannot ship without a state.
    //
    // None of them authorizes anything. The docs API is authorized with @PreAuthorize, which jEAP evaluates
    // against the roles in the token rather than against the ServletSemanticAuthorization bean the states
    // above mock - so the role is carried by the token authenticate() injects, and what these states set up is
    // data and nothing else. That the resources are really behind the role is DocsApiSecurityIT's subject.

    /**
     * Which state serves which route, as a route-to-state mapping. The keys are the routes as the controllers
     * declare them, with their path variables.
     */
    public static final Map<String, String> DOCS_API_ROUTE_STATES = Map.of(
            "/docs-api/systems", "A documented landscape is available",
            "/docs-api/systems/{system}", "A documented landscape is available",
            "/docs-api/systems/{system}/messages", "A documented landscape is available",
            "/docs-api/openapi-specs", "An OpenAPI specification is published",
            "/docs-api/database-schemas", "A database schema is published",
            "/docs-api/systems/{system}/components/{component}/openapi", "An OpenAPI specification is published",
            "/docs-api/systems/{system}/components/{component}/database-schema", "A database schema is published",
            "/docs-api/message-types", "A message type version is published",
            "/docs-api/message-types/{system}/{message}/versions/{version}", "A message type version is published");

    /**
     * The three model resources: the system list, one system, and its messages.
     * <p>
     * One state for all three because they are three views of one landscape - see {@link DocsApiPactStubs}.
     */
    @State("A documented landscape is available")
    void aDocumentedLandscapeIsAvailable() {
        when(architectureModelRepository.load()).thenReturn(DocsApiPactStubs.model());
    }

    /**
     * A system that is not in the model. The consumer's importer treats the {@code 404} as "this system is
     * gone" rather than as a failure, so the answer is part of the contract.
     */
    @State("No system named 'no-such-system' is documented")
    void noSuchSystemIsDocumented() {
        when(architectureModelRepository.load()).thenReturn(DocsApiPactStubs.model());
    }

    /**
     * The OpenAPI index and the specification it points at - one state, because the index promises the entity
     * tag of the content resource and the two must therefore come from the same bytes.
     */
    @State("An OpenAPI specification is published")
    void anOpenApiSpecificationIsPublished() {
        SystemComponent component = documentedComponent();
        when(openApiSpecRepository.findIndexEntries())
                .thenReturn(List.of(DocsApiPactStubs.openApiIndexEntry()));
        when(openApiSpecRepository.findByProvider(component))
                .thenReturn(Optional.of(DocsApiPactStubs.openApiSpec(component)));
    }

    /**
     * The database schema index and the schema it points at, for the same reason.
     */
    @State("A database schema is published")
    void aDatabaseSchemaIsPublished() {
        SystemComponent component = documentedComponent();
        when(systemComponentDatabaseSchemaRepository.findIndexEntries())
                .thenReturn(List.of(DocsApiPactStubs.databaseSchemaIndexEntry()));
        when(systemComponentDatabaseSchemaRepository.findBySystemComponent(component))
                .thenReturn(Optional.of(DocsApiPactStubs.databaseSchema(component)));
    }

    /**
     * The message type index and one version with its schemas.
     */
    @State("A message type version is published")
    void aMessageTypeVersionIsPublished() {
        when(systemRepository.findByNameOrAliasIgnoreCase(DocsApiPactStubs.SYSTEM))
                .thenReturn(Optional.of(documentedSystem()));
        // The index is asked unfiltered - the consumer replicates every system of an environment
        when(messageTypeVersionRepository.findIndexEntries(null))
                .thenReturn(List.of(DocsApiPactStubs.messageTypeIndexEntry()));
        when(messageTypeVersionRepository.findVersions(DocsApiPactStubs.SYSTEM, DocsApiPactStubs.EVENT_NAME,
                DocsApiPactStubs.MESSAGE_VERSION))
                .thenReturn(List.of(DocsApiPactStubs.messageTypeVersion()));
    }

    /**
     * The component of the landscape, with the lookups the two artifact resources resolve it through. They
     * check that the component really belongs to the system in the path, so system and component have to be the
     * ones out of the same model rather than two mocks.
     */
    private SystemComponent documentedComponent() {
        System system = documentedSystem();
        SystemComponent component = system.findSystemComponent(DocsApiPactStubs.COMPONENT).orElseThrow();
        when(systemRepository.findByNameOrAliasIgnoreCase(DocsApiPactStubs.SYSTEM)).thenReturn(Optional.of(system));
        when(systemComponentRepository.findByNameIgnoreCase(DocsApiPactStubs.COMPONENT))
                .thenReturn(Optional.of(component));
        return component;
    }

    private System documentedSystem() {
        return DocsApiPactStubs.model().getSystems().getFirst();
    }

    @SuppressWarnings("SameParameterValue")
    private SystemComponent mockSystemAndComponent(String systemName, String systemComponentName) {
        System system = mock(System.class);
        when(system.getName()).thenReturn(systemName);
        SystemComponent systemComponent = mock(SystemComponent.class);
        when(systemComponent.getName()).thenReturn(systemComponentName);
        when(systemComponent.getParent()).thenReturn(system);
        when(system.findSystemComponent(systemComponentName)).thenReturn(Optional.of(systemComponent));
        when(systemRepository.findByNameContainingIgnoreCase(systemName)).thenReturn(Optional.of(system));
        return systemComponent;
    }

    private DatabaseSchema getFullDatabaseSchema() {
        Table tableA = Table.builder()
                .name("table_a")
                .columns(List.of(new TableColumn(COLUMN_A, "text", false)))
                .primaryKey(new TablePrimaryKey("pk_a", List.of(COLUMN_A)))
                .build();
        Table tableB = Table.builder()
                .name("table_b")
                .columns(List.of(new TableColumn("column_b", "text", false)))
                .columns(List.of(new TableColumn("column_c", "text", true)))
                .foreignKeys(List.of(TableForeignKey.builder().
                                name("fk_a_b")
                                .columnNames(List.of("column_b"))
                                .referencedColumnNames(List.of(COLUMN_A))
                                .build()))
                .build();
        return DatabaseSchema.builder()
                .name("test-schema")
                .version(TEST_VERSION)
                .tables(List.of(tableA, tableB))
                .build();
    }

    @Value
    private static class ApiDocVersionImpl implements ApiDocVersion {
        String system;
        String component;
        String version;
    }

    @Value
    private static class DatabaseSchemaVersionImpl implements DatabaseSchemaVersion {
        String system;
        String component;
        String version;
    }
}
