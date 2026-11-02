package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.assertThat;

final class ModelOnboardingAssertions {
    private ModelOnboardingAssertions() {
    }

    static ModelAssert assertModel(ArchitectureModel model) {
        return new ModelAssert(model);
    }

    static SystemAssert assertSystem(System system) {
        return new SystemAssert(system);
    }

    static TeamAssert assertTeam(Team team) {
        return new TeamAssert(team);
    }

    static ComponentAssert assertComponent(SystemComponent component) {
        return new ComponentAssert(component);
    }

    static final class ModelAssert extends AbstractAssert<ModelAssert, ArchitectureModel> {
        private ModelAssert(ArchitectureModel model) {
            super(model, ModelAssert.class);
            isNotNull();
        }

        ModelAssert hasOnlySystems(System... systems) {
            assertThat(actual.getSystems()).as("model systems").containsExactly(systems);
            return this;
        }

        ModelAssert hasOnlyTeams(Team... teams) {
            assertThat(actual.getTeams()).as("model teams").containsExactly(teams);
            return this;
        }

        ModelAssert hasOnlySystemNames(String... names) {
            assertThat(actual.getSystems()).as("model system names")
                    .extracting(System::getName).containsExactly(names);
            return this;
        }

        ModelAssert hasRemovedSystems(System... systems) {
            assertThat(actual.getRemovedSystems()).as("removed systems").containsExactly(systems);
            return this;
        }

        ModelAssert hasRemovedTeams(Team... teams) {
            assertThat(actual.getRemovedTeams()).as("removed teams").containsExactly(teams);
            return this;
        }

        ModelAssert hasNoRemovals() {
            return hasRemovedSystems().hasRemovedTeams();
        }

        ModelAssert resolvesToSystem(String nameOrAlias, String systemName) {
            var expected = actual.findSystemByNameIgnoreCase(systemName);
            assertThat(expected).as("system named '%s'", systemName).isPresent();
            assertThat(actual.findSystem(nameOrAlias)).as("system resolved by '%s'", nameOrAlias)
                    .contains(expected.orElseThrow());
            return this;
        }

        SystemAssert system(String name) {
            var system = actual.findSystemByNameIgnoreCase(name);
            assertThat(system).as("system named '%s'", name).isPresent();
            return assertSystem(system.orElseThrow());
        }

        TeamAssert team(String name) {
            var team = actual.findTeamByNameIgnoreCase(name);
            assertThat(team).as("team named '%s'", name).isPresent();
            return assertTeam(team.orElseThrow());
        }
    }

    static final class SystemAssert extends AbstractAssert<SystemAssert, System> {
        private SystemAssert(System system) {
            super(system, SystemAssert.class);
            isNotNull();
        }

        SystemAssert hasName(String name) {
            assertThat(actual.getName()).as("system name").isEqualTo(name);
            return this;
        }

        SystemAssert isOwnedBy(Team owner) {
            assertThat(actual.getDefaultOwner()).as("default owner of system '%s'", actual.getName()).isSameAs(owner);
            return this;
        }

        SystemAssert hasDescription(String description) {
            assertThat(actual.getDescription()).as("description of system '%s'", actual.getName()).isEqualTo(description);
            return this;
        }

        SystemAssert hasConfluenceLink(String link) {
            assertThat(actual.getConfluenceLink()).as("Confluence link of system '%s'", actual.getName()).isEqualTo(link);
            return this;
        }

        SystemAssert hasAliases(String... aliases) {
            assertThat(actual.getAliases()).as("aliases of system '%s'", actual.getName()).containsExactly(aliases);
            return this;
        }

        SystemAssert hasNoOptionalDetails() {
            return hasDescription(null).hasConfluenceLink(null).hasAliases();
        }

        SystemAssert hasComponents(SystemComponent... components) {
            assertThat(actual.getSystemComponents()).as("components of system '%s'", actual.getName())
                    .containsExactly(components);
            return this;
        }

        SystemAssert hasNoComponents() {
            return hasComponents();
        }
    }

    static final class TeamAssert extends AbstractAssert<TeamAssert, Team> {
        private TeamAssert(Team team) {
            super(team, TeamAssert.class);
            isNotNull();
        }

        TeamAssert hasName(String name) {
            assertThat(actual.getName()).as("team name").isEqualTo(name);
            return this;
        }

        TeamAssert hasContactAddress(String address) {
            assertThat(actual.getContactAddress()).as("contact address of team '%s'", actual.getName()).isEqualTo(address);
            return this;
        }

        TeamAssert hasConfluenceLink(String link) {
            assertThat(actual.getConfluenceLink()).as("Confluence link of team '%s'", actual.getName()).isEqualTo(link);
            return this;
        }

        TeamAssert hasJiraLink(String link) {
            assertThat(actual.getJiraLink()).as("Jira link of team '%s'", actual.getName()).isEqualTo(link);
            return this;
        }

        TeamAssert hasNoContactDetails() {
            return hasContactAddress(null).hasConfluenceLink(null).hasJiraLink(null);
        }
    }

    static final class ComponentAssert extends AbstractAssert<ComponentAssert, SystemComponent> {
        private ComponentAssert(SystemComponent component) {
            super(component, ComponentAssert.class);
            isNotNull();
        }

        ComponentAssert isOwnedBy(Team owner) {
            assertThat(actual.getOwnedBy()).as("owner of component '%s'", actual.getName()).isSameAs(owner);
            return this;
        }

        ComponentAssert isOwnedByDefaultOwnerOf(System system) {
            return isOwnedBy(system.getDefaultOwner());
        }

        ComponentAssert hasParent(System system) {
            assertThat(actual.getParent()).as("parent system of component '%s'", actual.getName()).isSameAs(system);
            return this;
        }
    }
}
