package ch.admin.bit.jeap.archrepo.metamodel;

import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The configuration is the master of systems and teams. Once onboarding has applied it, the model is sealed:
 * nothing else - no importer - may add or remove a system or a team. Everything below them stays open.
 */
class ArchitectureModelSealTest {

    private Team team;
    private System system;
    private System other;
    private ArchitectureModel model;

    @BeforeEach
    void setUp() {
        team = Team.builder().name("blue").build();
        system = System.builder().name("foo").defaultOwner(team).build();
        other = System.builder().name("bar").defaultOwner(team).build();
        SystemComponentFactory.createSystemComponent(system, "foo-service", Importer.DEPLOYMENT_LOG);
        model = ArchitectureModel.builder().teams(List.of(team)).systems(List.of(system, other)).build();
    }

    @Test
    void anUnsealedModelAcceptsNewAndRemovedSystemsAndTeams() {
        assertThatCode(() -> {
            model.addTeam(Team.builder().name("red").build());
            model.addSystem(System.builder().name("baz").build());
            model.removeSystem(other);
            model.removeTeam(team);
        }).doesNotThrowAnyException();
    }

    @Test
    void aSealedModelRejectsAddingASystem() {
        model.sealSystemsAndTeams();
        System baz = System.builder().name("baz").build();

        assertThatThrownBy(() -> model.addSystem(baz))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("baz").hasMessageContaining("archrepo.model");
        assertThat(model.getSystems()).containsExactly(system, other);
    }

    @Test
    void aSealedModelRejectsRemovingASystem() {
        model.sealSystemsAndTeams();

        assertThatThrownBy(() -> model.removeSystem(other))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("bar");
        assertThat(model.getSystems()).containsExactly(system, other);
        assertThat(model.getRemovedSystems()).isEmpty();
    }

    @Test
    void aSealedModelRejectsAddingOrRemovingATeam() {
        model.sealSystemsAndTeams();

        Team red = Team.builder().name("red").build();
        assertThatThrownBy(() -> model.addTeam(red))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("red");
        assertThatThrownBy(() -> model.removeTeam(team))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("blue");
        assertThat(model.getTeams()).containsExactly(team);
        assertThat(model.getRemovedTeams()).isEmpty();
    }

    @Test
    void sealingLeavesEverythingBelowSystemsOpen() {
        model.sealSystemsAndTeams();

        assertThatCode(() -> {
            SystemComponentFactory.createSystemComponent(other, "bar-service", Importer.GRAFANA);
            model.remove(other.findSystemComponent("bar-service").orElseThrow());
            model.removeAllByImporter(Importer.DEPLOYMENT_LOG);
            model.cleanup();
        }).doesNotThrowAnyException();
    }

    @Test
    void aSealedModelStillRelocatesAComponentToTheSystemItsNameBelongsTo() {
        // "bar-moved" belongs to bar by its name but sits in foo - as after an alias moved from foo to bar.
        var misplaced = SystemComponentFactory.createSystemComponent(system, "bar-moved", Importer.DEPLOYMENT_LOG);
        model.sealSystemsAndTeams();

        var found = model.findOrRelocateSystemComponent(other, "bar-moved");

        assertThat(found).containsSame(misplaced);
        assertThat(misplaced.getParent()).isSameAs(other);
        assertThat(system.findSystemComponent("bar-moved")).isEmpty();
        assertThat(model.getSystems()).containsExactly(system, other);
    }

    @Test
    void aSealedModelStillRevivesAComponentRemovedEarlierInTheSameUpdate() {
        var component = system.findSystemComponent("foo-service").orElseThrow();
        model.sealSystemsAndTeams();
        model.remove(component);

        var revived = model.findOrRelocateSystemComponent(system, "foo-service");

        assertThat(revived).containsSame(component);
        assertThat(system.getSystemComponentsIncludingUndeployed()).containsExactly(component);
        assertThat(model.getRemovedSystemComponents()).isEmpty();
        assertThat(model.getSystems()).containsExactly(system, other);
    }
}
