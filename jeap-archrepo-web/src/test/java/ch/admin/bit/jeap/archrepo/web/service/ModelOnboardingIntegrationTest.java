package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentFactory;
import ch.admin.bit.jeap.archrepo.persistence.ArchitectureModelRepository;
import ch.admin.bit.jeap.archrepo.web.ArchRepoModelProperties;
import ch.admin.bit.jeap.archrepo.web.PostgresIntegrationTestBase;
import ch.admin.bit.jeap.archrepo.web.rest.model.ArchRepoTestConfiguration;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import jakarta.persistence.EntityManager;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ArchRepoTestConfiguration.class, DisableJeapPermitAllSecurityConfiguration.class})
@Transactional
class ModelOnboardingIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired
    UpdateService updateService;
    @Autowired
    ArchitectureModelRepository repository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    MockMvc mvc;
    @MockitoBean
    ArchRepoImporter importer;
    @MockitoBean
    ArchRepoModelProperties properties;
    /**
     * Always grants the model update lock. This test is about onboarding inside a test-managed transaction;
     * ShedLock takes its lock in a separate transaction on its own connection, which does not mix with that.
     * The lock itself is covered by UpdateServiceUpdateModelTest.
     */
    @MockitoBean
    LockProvider lockProvider;

    @BeforeEach
    void grantModelUpdateLock() {
        when(lockProvider.lock(any())).thenReturn(Optional.of(() -> { }));
    }

    @Test
    void configOnlySystemsAppearInApisAndDeletionAndRecreationConverge() throws Exception {
        when(properties.teams()).thenReturn(List.of(new ArchRepoModelProperties.TeamDefinition("onboard-team", null, null, null)));
        var empty = definition("onboard-empty");
        var live = definition("onboard-live");
        when(properties.systems()).thenReturn(List.of(empty, live));
        doAnswer(invocation -> {
            ArchitectureModel model = invocation.getArgument(0);
            model.findSystem("onboard-live").ifPresent(system -> {
                if (system.getSystemComponents().isEmpty()) {
                    SystemComponentFactory.createSystemComponent(system, "onboard-live-service", Importer.DEPLOYMENT_LOG);
                }
            });
            return null;
        }).when(importer).importIntoModel(any(), eq("dev"));

        updateService.runImporter(importer.getClass().getSimpleName());
        entityManager.clear();
        var loaded = repository.load();
        var originalId = loaded.findSystem("onboard-live").orElseThrow().getId();
        assertThat(loaded.findSystemComponent("onboard-live-service").orElseThrow().getOwnedBy().getName())
                .isEqualTo("onboard-team");
        var token = JeapAuthenticationTestTokenBuilder.create().withUserRoles(SemanticApplicationRole.builder()
                .system("application-platform").resource("architecture-model").operation("read").build()).build();
        mvc.perform(get("/docs-api/systems").with(authentication(token)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("onboard-empty")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("onboard-live")));
        mvc.perform(get("/docs-api/systems/onboard-empty").with(authentication(token)))
                .andExpect(status().isOk());

        updateService.runImporter(importer.getClass().getSimpleName());
        entityManager.clear();
        assertThat(repository.load().getSystems()).hasSize(2);
        assertThat(repository.load().getTeams()).hasSize(1);
        assertThat(repository.load().findSystem("onboard-live").orElseThrow().getId()).isEqualTo(originalId);

        when(properties.systems()).thenReturn(List.of(empty));
        updateService.runImporter(importer.getClass().getSimpleName());
        entityManager.clear();
        mvc.perform(get("/api/model")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("onboard-live"))));
        mvc.perform(get("/docs-api/systems/onboard-live").with(authentication(token)))
                .andExpect(status().isNotFound());

        when(properties.systems()).thenReturn(List.of(live, empty));
        updateService.runImporter(importer.getClass().getSimpleName());
        entityManager.clear();
        var recreated = repository.load().findSystem("onboard-live").orElseThrow();
        assertThat(recreated.getId()).isNotEqualTo(originalId);
        assertThat(recreated.getSystemComponents()).hasSize(1);
    }

    @Test
    void renameIntoANewSystemKeepsTheComponentWithItsArtifactsAndInboundRelations() {
        UUID componentId = seed("renold", List.of(), "renold-service", true);
        configure(definition("rennew", "renold"), definition(CONSUMER));
        importerReports("renold", "renold-service");

        runOnboardingAndImporter();

        assertSystemNames("rennew", CONSUMER);
        assertComponentWithArtifactsIn("rennew", "renold-service", componentId);
    }

    @ParameterizedTest(name = "old system stored first: {0}")
    @CsvSource({"true", "false"})
    void renameIntoAnExistingSystemKeepsTheComponentWithItsArtifactsAndInboundRelations(boolean ownerFirst) {
        UUID componentId = seed("mergeold", List.of(), "mergeold-service", ownerFirst, "mergenew");
        configure(definition("mergenew", "mergeold"), definition(CONSUMER));
        importerReports("mergeold", "mergeold-service");

        runOnboardingAndImporter();

        assertSystemNames("mergenew", CONSUMER);
        assertComponentWithArtifactsIn("mergenew", "mergeold-service", componentId);
    }

    @ParameterizedTest(name = "alias moves from {0} to {1}, owner stored first: {2}")
    @CsvSource({"movea,movez,true", "movea,movez,false", "movez,movea,true", "movez,movea,false"})
    void aliasMovedBetweenRetainedSystemsRelocatesTheComponentWithItsArtifacts(String from, String to, boolean ownerFirst) {
        // An alias still held by a retained system cannot be taken over in the same update, so moving it takes
        // two deployments: the first releases it, the second assigns it to the other system.
        UUID componentId = seed(from, List.of("moved"), "moved-service", ownerFirst, to);
        importerReports("moved", "moved-service");
        configure(definition(from), definition(to), definition(CONSUMER));
        runOnboardingAndImporter();
        configure(definition(from), definition(to, "moved"), definition(CONSUMER));

        runOnboardingAndImporter();

        assertSystemNames(from, to, CONSUMER);
        assertComponentWithArtifactsIn(to, "moved-service", componentId);
        System previousOwner = repository.load().findSystemByNameIgnoreCase(from).orElseThrow();
        assertThat(previousOwner.getSystemComponentsIncludingUndeployed()).isEmpty();
        assertThat(previousOwner.getRestApis()).isEmpty();
        assertThat(previousOwner.getOpenApiSpecs()).isEmpty();
        assertThat(previousOwner.getDatabaseSchemas()).isEmpty();
    }

    @Test
    void aComponentOfASystemDeletedInThisRunCanBeRecreatedUnderAnotherSystemInTheSameRun() {
        // An importer that resolves the system independently of the component name (RHOS uses the namespace)
        // reports the deleted system's component under a retained one. The deleted row must be gone before the
        // importer's new row is written, or the global unique index on the component name rejects the update.
        UUID componentId = seed("dropped", List.of(), "dropped-service", true, "adopter");
        configure(definition("adopter"), definition(CONSUMER));
        importerReports("adopter", "dropped-service");

        runOnboardingAndImporter();

        assertSystemNames("adopter", CONSUMER);
        var recreated = repository.load().findSystemByNameIgnoreCase("adopter").orElseThrow()
                .findSystemComponent("dropped-service").orElseThrow();
        assertThat(recreated.getId()).isNotEqualTo(componentId);
    }

    private static final String TEAM = "onboard-team";
    private static final String CONSUMER = "consumer";
    private static final byte[] CONTENT = "{}".getBytes(StandardCharsets.UTF_8);

    /**
     * Stores {@code owner} with {@code component}, which provides a REST API, an OpenAPI spec and a DB schema,
     * a {@value CONSUMER} system whose component consumes that REST API, and any further empty systems.
     */
    private UUID seed(String owner, List<String> ownerAliases, String component, boolean ownerFirst, String... others) {
        Team team = Team.builder().name(TEAM).build();
        List<System> systems = new ArrayList<>();
        System ownerSystem = System.builder().name(owner).defaultOwner(team).aliases(ownerAliases).build();
        System consumer = System.builder().name(CONSUMER).defaultOwner(team).build();
        SystemComponentFactory.createSystemComponent(ownerSystem, component, Importer.DEPLOYMENT_LOG);
        SystemComponentFactory.createSystemComponent(consumer, CONSUMER + "-service", Importer.DEPLOYMENT_LOG);
        for (String other : others) {
            systems.add(System.builder().name(other).defaultOwner(team).build());
        }
        systems.add(ownerFirst ? 0 : systems.size(), ownerSystem);
        systems.add(consumer);
        repository.save(ArchitectureModel.builder().teams(List.of(team)).systems(systems).build());
        entityManager.flush();
        entityManager.clear();

        var model = repository.load();
        System stored = model.findSystemByNameIgnoreCase(owner).orElseThrow();
        SystemComponent provider = stored.findSystemComponent(component).orElseThrow();
        RestApi restApi = RestApi.builder().provider(provider).method("GET").path("/things").importer(Importer.OPEN_API).build();
        stored.addRestApi(restApi);
        stored.addOpenApiSpec(OpenApiSpec.builder().provider(provider).version("1").content(CONTENT).build());
        stored.addDatabaseSchema(SystemComponentDatabaseSchema.builder()
                .systemComponent(provider).schema(CONTENT).schemaVersion("1").build());
        model.findSystemByNameIgnoreCase(CONSUMER).orElseThrow().addRelation(RestApiRelation.builder()
                .restApi(restApi).consumerName(CONSUMER + "-service").lastSeen(ZonedDateTime.now()).build());
        repository.save(model);
        entityManager.flush();
        entityManager.clear();
        return provider.getId();
    }

    private void configure(ArchRepoModelProperties.SystemDefinition... systems) {
        when(properties.teams()).thenReturn(List.of(new ArchRepoModelProperties.TeamDefinition(TEAM, null, null, null)));
        when(properties.systems()).thenReturn(List.of(systems));
    }

    /**
     * Behaves like the system component importers: resolve the system, then relocate or create the component;
     * skip the component while no system resolves.
     */
    private void importerReports(String systemNameOrAlias, String component) {
        doAnswer(invocation -> {
            ArchitectureModel model = invocation.getArgument(0);
            model.findSystem(systemNameOrAlias).ifPresent(system -> {
                if (model.findOrRelocateSystemComponent(system, component).isEmpty()) {
                    SystemComponentFactory.createSystemComponent(system, component, Importer.DEPLOYMENT_LOG);
                }
            });
            return null;
        }).when(importer).importIntoModel(any(), eq("dev"));
    }

    private void runOnboardingAndImporter() {
        updateService.runImporter(importer.getClass().getSimpleName());
        entityManager.flush();
        entityManager.clear();
    }

    private void assertSystemNames(String... names) {
        assertThat(repository.load().getSystems()).extracting(System::getName).containsExactlyInAnyOrder(names);
    }

    private void assertComponentWithArtifactsIn(String systemName, String component, UUID componentId) {
        var model = repository.load();
        System system = model.findSystemByNameIgnoreCase(systemName).orElseThrow();
        SystemComponent moved = system.findSystemComponent(component).orElseThrow();
        assertThat(moved.getId()).as("component keeps its identity").isEqualTo(componentId);
        assertThat(system.getRestApis()).as("REST APIs").extracting(RestApi::getProvider).containsExactly(moved);
        assertThat(system.getOpenApiSpecs()).as("OpenAPI specs").extracting(OpenApiSpec::getProvider).containsExactly(moved);
        assertThat(system.getDatabaseSchema(component)).as("DB schema").isPresent();
        assertThat(model.getRestApiForSystemComponent(moved)).as("Swagger link").isPresent();
        assertThat(model.getAllRelationsByType(RestApiRelation.class)).as("inbound REST relation")
                .singleElement().satisfies(relation -> {
                    assertThat(relation.getProviderName()).isEqualTo(component);
                    assertThat(relation.getConsumerName()).isEqualTo(CONSUMER + "-service");
                });
    }

    private static ArchRepoModelProperties.SystemDefinition definition(String name, String... aliases) {
        return new ArchRepoModelProperties.SystemDefinition(name, TEAM, null, null, List.of(aliases));
    }
}
