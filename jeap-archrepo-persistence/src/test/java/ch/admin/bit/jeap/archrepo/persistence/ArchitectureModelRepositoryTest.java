package ch.admin.bit.jeap.archrepo.persistence;

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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class ArchitectureModelRepositoryTest extends PostgresDataJpaTestBase {
    @Autowired
    TeamRepository teams;
    @Autowired
    SystemRepository systems;
    @Autowired
    EntityManager entityManager;
    ArchitectureModelRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ArchitectureModelRepository(teams, systems, entityManager);
        ReflectionTestUtils.setField(repository, "openApiBaseUrl", "https://example.ch/");
    }

    @Test
    void savesNewTeamsBeforeSystemsAndPreservesDetails() {
        Team team = Team.builder().name("blue").contactAddress("contact").build();
        System system = System.builder().name("foo").description("description").defaultOwner(team)
                .aliases(List.of("fo")).build();
        repository.save(ArchitectureModel.builder().teams(List.of(team)).systems(List.of(system)).build());
        entityManager.clear();

        var loaded = repository.load();
        assertThat(loaded.getTeams()).hasSize(1);
        assertThat(loaded.getTeams().getFirst().getContactAddress()).isEqualTo("contact");
        var saved = loaded.getSystems().getFirst();
        assertThat(saved.getId()).isEqualTo(system.getId());
        assertThat(saved.getDescription()).isEqualTo("description");
        assertThat(saved.getAliases()).containsExactly("fo");
        assertThat(saved.getDefaultOwner().getId()).isEqualTo(team.getId());
    }

    @Test
    void deletesAggregateAfterCrossSystemRelationsAndReleasesRemovedTeam() {
        Team removedTeam = teams.save(Team.builder().name("removed-team").build());
        Team keptTeam = teams.save(Team.builder().name("kept-team").build());
        System removed = System.builder().name("removed").defaultOwner(removedTeam).build();
        System kept = System.builder().name("kept").defaultOwner(keptTeam).build();
        System other = System.builder().name("other").defaultOwner(keptTeam).build();
        var provider = SystemComponentFactory.createSystemComponent(removed, "removed-provider-service", Importer.DEPLOYMENT_LOG);
        var consumer = SystemComponentFactory.createSystemComponent(kept, "kept-consumer-service", Importer.DEPLOYMENT_LOG);
        consumer.setOwnedBy(removedTeam);
        var otherProvider = SystemComponentFactory.createSystemComponent(other, "other-provider-service", Importer.DEPLOYMENT_LOG);
        RestApi removedApi = RestApi.builder().provider(provider).method("GET").path("/removed").build();
        removed.addRestApi(removedApi);
        RestApi keptApi = RestApi.builder().provider(otherProvider).method("GET").path("/kept").build();
        other.addRestApi(keptApi);
        kept.addRelation(RestApiRelation.builder().restApi(removedApi).consumerName(consumer.getName()).build());
        kept.addRelation(RestApiRelation.builder().restApi(keptApi).consumerName(consumer.getName()).build());
        repository.save(ArchitectureModel.builder().teams(List.of(removedTeam, keptTeam))
                .systems(List.of(removed, kept, other)).build());
        entityManager.clear();

        var model = repository.load();
        System toDelete = model.findSystem("removed").orElseThrow();
        toDelete.addDatabaseSchema(SystemComponentDatabaseSchema.builder()
                .systemComponent(toDelete.getSystemComponents().getFirst())
                .schema("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8)).schemaVersion("1").build());
        repository.save(model);
        Team teamToDelete = model.findTeamByNameIgnoreCase("removed-team").orElseThrow();
        var component = model.findSystemComponent("kept-consumer-service").orElseThrow();
        model.removeSystem(toDelete);
        component.setOwnedBy(component.getParent().getDefaultOwner());
        model.removeTeam(teamToDelete);
        repository.save(model);
        entityManager.clear();

        var after = repository.load();
        assertThat(after.getSystems()).extracting(System::getName).containsExactlyInAnyOrder("kept", "other");
        assertThat(after.getTeams()).extracting(Team::getName).containsExactly("kept-team");
        assertThat(after.getAllRelations()).hasSize(1);
        assertThat(after.getAllRelations().getFirst().getProviderName()).isEqualTo("other-provider-service");
        assertThat(after.findSystemComponent("kept-consumer-service").orElseThrow().getOwnedBy().getName())
                .isEqualTo("kept-team");
        assertThat(entityManager.createQuery("select count(s) from SystemComponentDatabaseSchema s", Long.class)
                .getSingleResult()).isZero();
    }

    @Test
    void removedComponentAndItsArtifactsAreDeletedNotLeftBehind() {
        storeOwnerWithArtifacts();

        var model = repository.load();
        model.remove(model.findSystemComponent("owner-service").orElseThrow());
        repository.save(model);
        entityManager.flush();
        entityManager.clear();

        assertThat(count("SystemComponent")).isZero();
        assertThat(count("RestApi")).isZero();
        assertThat(count("OpenApiSpec")).isZero();
        assertThat(count("SystemComponentDatabaseSchema")).isZero();
        assertThat(repository.load().getSystems()).extracting(System::getName).containsExactlyInAnyOrder("owner", "other");
    }

    @Test
    void unusedRestApiRemovedByCleanupIsDeleted() {
        storeOwnerWithArtifacts();

        var model = repository.load();
        model.cleanup();
        repository.save(model);
        entityManager.flush();
        entityManager.clear();

        assertThat(count("RestApi")).isZero();
        assertThat(count("SystemComponent")).isOne();
    }

    @Test
    void componentRemovedAndSeenAgainInTheSameRunKeepsItsSingleRow() {
        SystemComponent original = storeOwnerWithArtifacts();

        var model = repository.load();
        model.remove(model.findSystemComponent("owner-service").orElseThrow());
        var revived = model.findOrRelocateSystemComponent(model.findSystem("other").orElseThrow(), "owner-service");
        repository.save(model);
        entityManager.flush();
        entityManager.clear();

        assertThat(revived).isPresent();
        var stored = repository.load().findSystemComponent("owner-service").orElseThrow();
        assertThat(stored.getId()).isEqualTo(original.getId());
        assertThat(stored.getParent().getName()).as("revived into the system its name belongs to").isEqualTo("owner");
        assertThat(count("SystemComponent")).isOne();
        assertThat(count("RestApi")).as("its artifacts stay removed").isZero();
    }

    private SystemComponent storeOwnerWithArtifacts() {
        Team team = teams.save(Team.builder().name("blue").build());
        System owner = System.builder().name("owner").defaultOwner(team).build();
        System other = System.builder().name("other").defaultOwner(team).build();
        SystemComponent component = SystemComponentFactory.createSystemComponent(owner, "owner-service", Importer.DEPLOYMENT_LOG);
        repository.save(ArchitectureModel.builder().teams(List.of(team)).systems(List.of(owner, other)).build());
        entityManager.flush();
        entityManager.clear();

        var model = repository.load();
        System stored = model.findSystem("owner").orElseThrow();
        SystemComponent provider = stored.findSystemComponent("owner-service").orElseThrow();
        byte[] content = "{}".getBytes(StandardCharsets.UTF_8);
        stored.addRestApi(RestApi.builder().provider(provider).method("GET").path("/things").importer(Importer.GRAFANA).build());
        stored.addOpenApiSpec(OpenApiSpec.builder().provider(provider).content(content).build());
        stored.addDatabaseSchema(SystemComponentDatabaseSchema.builder().systemComponent(provider).schema(content).schemaVersion("1").build());
        entityManager.flush();
        entityManager.clear();
        return component;
    }

    private long count(String entity) {
        return entityManager.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult();
    }

    @Test
    void newRestApiReferencedByANewRelationInAnotherSystemIsSavedAsTheModelsOwnInstance() {
        // Regression: merging each loaded system on its own made Hibernate copy the new REST API once per merge,
        // and the second merge copied the original's unset created_at onto the managed copy.
        storeOwnerWithArtifacts();
        var model = repository.load();
        SystemComponent provider = model.findSystemComponent("owner-service").orElseThrow();
        System consumer = model.findSystem("other").orElseThrow();
        SystemComponent consumerComponent = SystemComponentFactory.createSystemComponent(consumer, "other-service", Importer.PACT_BROKER);
        RestApi restApi = RestApi.builder().provider(provider).method("POST").path("/orders").importer(Importer.PACT_BROKER).build();
        provider.getParent().addRestApi(restApi);
        RestApiRelation relation = RestApiRelation.builder().restApi(restApi).consumerName(consumerComponent.getName())
                .importer(Importer.PACT_BROKER).lastSeen(java.time.ZonedDateTime.now()).build();
        consumer.addRelation(relation);

        repository.save(model);

        assertThat(entityManager.contains(restApi)).as("the model's REST API is the persisted one, not a copy").isTrue();
        assertThat(entityManager.contains(relation)).isTrue();
        entityManager.flush();
        entityManager.clear();
        var reloaded = repository.load();
        assertThat(reloaded.getAllRelationsByType(RestApiRelation.class)).singleElement()
                .satisfies(saved -> assertThat(saved.getRestApi().getPath()).isEqualTo("/orders"));
    }

    @Test
    void aDetachedModelIsStillSaved() {
        storeOwnerWithArtifacts();
        var model = repository.load();
        entityManager.clear();
        model.findSystem("owner").orElseThrow().updateDetails("changed while detached", null,
                model.findSystem("owner").orElseThrow().getDefaultOwner());

        repository.save(model);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.load().findSystem("owner").orElseThrow().getDescription()).isEqualTo("changed while detached");
    }

    @Test
    void savingPartialModelNeverImplicitlyDeletesOtherRows() {
        Team team = teams.save(Team.builder().name("blue").build());
        systems.saveAndFlush(System.builder().name("existing").defaultOwner(team).build());
        repository.save(ArchitectureModel.builder().build());
        entityManager.clear();
        assertThat(repository.load().getSystems()).extracting(System::getName).containsExactly("existing");
        assertThat(repository.load().getTeams()).extracting(Team::getName).containsExactly("blue");
    }
}
