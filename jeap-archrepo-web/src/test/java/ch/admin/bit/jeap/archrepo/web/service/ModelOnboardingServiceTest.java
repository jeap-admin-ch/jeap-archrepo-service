package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentFactory;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.web.ArchRepoModelProperties;
import ch.admin.bit.jeap.archrepo.web.ArchRepoModelProperties.SystemDefinition;
import ch.admin.bit.jeap.archrepo.web.ArchRepoModelProperties.TeamDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ch.admin.bit.jeap.archrepo.web.service.ModelOnboardingAssertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelOnboardingServiceTest {
    @Test
    void createsCompleteDeclarationAndIsIdempotent() {
        var properties = new ArchRepoModelProperties(List.of(new TeamDefinition("Team Blue", "contact", "team-link", "jira")),
                List.of(new SystemDefinition("foo", "TEAM BLUE", "description", "system-link", List.of("fo"))));
        var model = ArchitectureModel.builder().build();
        var service = new ModelOnboardingService(properties);
        service.apply(model);
        var system = model.getSystems().getFirst();
        var team = model.getTeams().getFirst();
        service.apply(model);
        assertModel(model)
                .hasOnlySystems(system)
                .hasOnlyTeams(team);
        assertSystem(system)
                .isOwnedBy(team)
                .hasDescription("description")
                .hasConfluenceLink("system-link")
                .hasAliases("fo");
        assertTeam(team)
                .hasContactAddress("contact")
                .hasConfluenceLink("team-link")
                .hasJiraLink("jira");
    }

    @Test
    void normalizesCaseWithoutRecreatingAndClearsAbsentFields() {
        Team team = Team.builder().name("BLUE").contactAddress("old").confluenceLink("old").jiraLink("old").build();
        System system = System.builder().name("FOO").defaultOwner(team).description("old")
                .confluenceLink("old").aliases(List.of("old")).build();
        var component = SystemComponentFactory.createSystemComponent(system, "foo-service", Importer.DEPLOYMENT_LOG);
        var model = ArchitectureModel.builder().systems(List.of(system)).teams(List.of(team)).build();
        service(List.of(team("blue")), List.of(system("foo", "blue"))).apply(model);
        assertModel(model).hasNoRemovals();
        assertModel(model).system("foo")
                .isSameAs(system)
                .hasName("foo")
                .hasNoOptionalDetails()
                .hasComponents(component);
        assertModel(model).team("blue")
                .isSameAs(team)
                .hasName("blue")
                .hasNoContactDetails();
    }

    @Test
    void keepsComponentOwnershipUnlessTheTeamIsDeleted() {
        Team old = Team.builder().name("old").build();
        System system = System.builder().name("foo").defaultOwner(old).build();
        var component = SystemComponentFactory.createSystemComponent(system, "foo-service", Importer.DEPLOYMENT_LOG);
        var model = ArchitectureModel.builder().teams(List.of(old)).systems(List.of(system)).build();

        service(List.of(team("old"), team("new")), List.of(system("foo", "new"))).apply(model);

        assertComponent(component).isOwnedBy(old);
        assertModel(model).team("new").isSameAs(system.getDefaultOwner());

        service(List.of(team("new")), List.of(system("foo", "new"))).apply(model);

        assertComponent(component).isOwnedByDefaultOwnerOf(system);
        assertModel(model).hasRemovedTeams(old);
    }

    @Test
    void renameFreesTheOldAggregateButMovesItsComponentsToTheSystemItsNameNowAliases() {
        Team owner = Team.builder().name("blue").build();
        System old = System.builder().name("foo").defaultOwner(owner).aliases(List.of("fo")).build();
        var component = SystemComponentFactory.createSystemComponent(old, "foo-service", Importer.DEPLOYMENT_LOG);
        var model = ArchitectureModel.builder().teams(List.of(owner)).systems(List.of(old)).build();
        service(List.of(team("blue")), List.of(system("bar", "blue", "foo", "fo")))
                .apply(model);
        assertModel(model)
                .hasRemovedSystems(old)
                .hasOnlySystemNames("bar")
                .resolvesToSystem("FOO", "bar");
        assertModel(model).system("bar").hasComponents(component);
        assertComponent(component).hasParent(model.findSystemByNameIgnoreCase("bar").orElseThrow());
    }

    @Test
    void renameAlsoMovesComponentsWhenOnlyAFormerAliasOfTheOldSystemIsReused() {
        Team owner = Team.builder().name("blue").build();
        System old = System.builder().name("foo").defaultOwner(owner).aliases(List.of("fo")).build();
        var component = SystemComponentFactory.createSystemComponent(old, "foo-service", Importer.DEPLOYMENT_LOG);
        var model = ArchitectureModel.builder().teams(List.of(owner)).systems(List.of(old)).build();
        service(List.of(team("blue")), List.of(system("bar", "blue", "fo")))
                .apply(model);
        assertModel(model).system("bar").hasComponents(component);
    }

    @Test
    void renameAlsoMovesComponentsWhenAFormerAliasBecomesTheNewName() {
        // Names and aliases share one namespace, so a system taking over the old alias as its name continues the
        // removed system just as one listing it as an alias does.
        Team owner = Team.builder().name("blue").build();
        System old = System.builder().name("foo").defaultOwner(owner).aliases(List.of("fo")).build();
        var component = SystemComponentFactory.createSystemComponent(old, "foo-service", Importer.DEPLOYMENT_LOG);
        var model = ArchitectureModel.builder().teams(List.of(owner)).systems(List.of(old)).build();

        service(List.of(team("blue")), List.of(system("FO", "blue"))).apply(model);

        assertModel(model).hasRemovedSystems(old).hasOnlySystemNames("FO");
        assertModel(model).system("fo").hasComponents(component);
    }

    @Test
    void renameIsRejectedWhenTwoSystemsClaimTheIdentityOfTheSameRemovedSystem() {
        Team owner = Team.builder().name("blue").build();
        System old = System.builder().name("foo").defaultOwner(owner).aliases(List.of("fo")).build();
        SystemComponentFactory.createSystemComponent(old, "foo-service", Importer.DEPLOYMENT_LOG);
        var model = ArchitectureModel.builder().teams(List.of(owner)).systems(List.of(old)).build();

        assertThatThrownBy(() -> service(List.of(team("blue")),
                List.of(system("fo", "blue"), system("bar", "blue", "foo"))).apply(model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("foo").hasMessageContaining("fo").hasMessageContaining("bar");
        assertModel(model).hasOnlySystems(old).hasNoRemovals();
    }

    @Test
    void plainDeletionDropsComponentsWhenNoSurvivingSystemClaimsTheOldNameOrAliasesAsItsOwn() {
        Team owner = Team.builder().name("blue").build();
        System old = System.builder().name("foo").defaultOwner(owner).aliases(List.of("fo")).build();
        SystemComponentFactory.createSystemComponent(old, "foo-service", Importer.DEPLOYMENT_LOG);
        var model = ArchitectureModel.builder().teams(List.of(owner)).systems(List.of(old)).build();
        service(List.of(team("blue")), List.of(system("bar", "blue")))
                .apply(model);
        assertModel(model)
                .hasRemovedSystems(old)
                .hasOnlySystemNames("bar");
        assertModel(model).system("bar").hasNoComponents();
    }

    @Test
    void renameFailsIfTheSurvivingSystemAlreadyHasAComponentOfTheSameName() {
        Team owner = Team.builder().name("blue").build();
        System old = System.builder().name("foo").defaultOwner(owner).build();
        SystemComponentFactory.createSystemComponent(old, "shared-service", Importer.DEPLOYMENT_LOG);
        System bar = System.builder().name("bar").defaultOwner(owner).build();
        SystemComponentFactory.createSystemComponent(bar, "shared-service", Importer.DEPLOYMENT_LOG);
        var model = ArchitectureModel.builder().teams(List.of(owner)).systems(List.of(old, bar)).build();

        assertThatThrownBy(() -> service(List.of(team("blue")), List.of(system("bar", "blue", "foo")))
                .apply(model))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shared-service");
    }

    @Test
    void missingOrPartialConfigurationIsAnEntireNoOpEvenIfOtherListIsInvalid() {
        Team owner = Team.builder().name("blue").build();
        System system = System.builder().name("foo").defaultOwner(owner).description("keep").build();
        var model = ArchitectureModel.builder().teams(List.of(owner)).systems(List.of(system)).build();
        for (var properties : List.of(new ArchRepoModelProperties(null, null),
                new ArchRepoModelProperties(List.of(team("duplicate"), team("DUPLICATE")), null),
                new ArchRepoModelProperties(null, List.of(system("foo", "unknown"))))) {
            new ModelOnboardingService(properties).apply(model);
            assertModel(model)
                    .hasOnlySystems(system)
                    .hasOnlyTeams(owner)
                    .hasNoRemovals();
            assertSystem(system).hasDescription("keep");
        }
    }

    @Test
    void validatesAgainstTheStoredModelBeforeAnyMutation() {
        // Configuration-only errors cannot reach apply(): ArchRepoModelProperties rejects them at startup (see
        // ArchRepoModelPropertiesTest). What is left are conflicts with the stored model, checked before any change.
        Team owner = Team.builder().name("blue").contactAddress("keep").build();
        System existing = System.builder().name("foo").description("keep").aliases(List.of("shared"))
                .defaultOwner(owner).build();
        var model = ArchitectureModel.builder().teams(List.of(owner)).systems(List.of(existing)).build();
        assertThatThrownBy(() -> service(List.of(team("blue")),
                List.of(system("foo", "blue", "shared"), system("SHARED", "blue"))).apply(model))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("SHARED", "foo");
        assertSystem(existing).hasDescription("keep").hasAliases("shared");
        assertTeam(owner).hasContactAddress("keep");
        assertModel(model).hasOnlySystems(existing).hasNoRemovals();
    }

    @Test
    void rejectsLegacyAmbiguityInsteadOfPickingARow() {
        var service = service(List.of(team("blue")), List.of(system("foo", "blue")));
        var duplicateSystems = ArchitectureModel.builder().systems(List.of(
                System.builder().name("foo").build(), System.builder().name("FOO").build())).build();
        assertThatThrownBy(() -> service.apply(duplicateSystems)).hasMessageContaining("foo", "FOO");
        var duplicateTeams = ArchitectureModel.builder().teams(List.of(
                Team.builder().name("blue").build(), Team.builder().name("BLUE").build())).build();
        assertThatThrownBy(() -> service.apply(duplicateTeams)).hasMessageContaining("blue", "BLUE");
        var duplicateAliases = ArchitectureModel.builder().systems(List.of(
                System.builder().name("foo").aliases(List.of("shared")).build(),
                System.builder().name("bar").aliases(List.of("SHARED")).build())).build();
        assertThatThrownBy(() -> service.apply(duplicateAliases)).hasMessageContaining("foo", "bar", "SHARED");
    }

    @Test
    void cannotTakeAnAliasFromASystemThatRemains() {
        var model = ArchitectureModel.builder().systems(List.of(
                System.builder().name("foo").aliases(List.of("shared")).build())).build();
        assertThatThrownBy(() -> service(List.of(team("blue")),
                List.of(system("foo", "blue"), system("SHARED", "blue"))).apply(model))
                .hasMessageContaining("SHARED", "foo");
        assertModel(model).hasOnlySystemNames("foo");
    }

    private static ModelOnboardingService service(List<TeamDefinition> teams, List<SystemDefinition> systems) {
        return new ModelOnboardingService(new ArchRepoModelProperties(teams, systems));
    }

    private static TeamDefinition team(String name) {
        return new TeamDefinition(name, null, null, null);
    }

    private static SystemDefinition system(String name, String team, String... aliases) {
        return new SystemDefinition(name, team, null, null, List.of(aliases));
    }
}
