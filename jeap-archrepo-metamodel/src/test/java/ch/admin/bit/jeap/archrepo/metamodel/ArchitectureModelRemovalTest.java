package ch.admin.bit.jeap.archrepo.metamodel;

import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.relation.CommandRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureModelRemovalTest {
    @Test
    void removesEveryKindOfCrossSystemReferenceIncludingUndeployedComponents() {
        System removed = System.builder().name("removed").build();
        System remaining = System.builder().name("remaining").build();
        var provider = SystemComponentFactory.createSystemComponent(removed, "removed-provider", Importer.DEPLOYMENT_LOG);
        provider.markUndeployed(java.time.ZonedDateTime.now());
        var api = RestApi.builder().provider(provider).method("GET").path("/api").build();
        removed.addRestApi(api);
        removed.addEvent(Event.builder().messageTypeName("RemovedEvent").messageVersions(List.of())
                .scope("public").descriptorUrl("url").build());
        removed.addCommand(Command.builder().messageTypeName("RemovedCommand").messageVersions(List.of())
                .scope("public").descriptorUrl("url").build());
        remaining.addRelation(RestApiRelation.builder().restApi(api).consumerName("remaining-consumer").build());
        remaining.addRelation(EventRelation.builder().eventName("RemovedEvent").providerName("external").consumerName("other").build());
        remaining.addRelation(CommandRelation.builder().commandName("RemovedCommand").providerName("external").consumerName("other").build());
        remaining.addRelation(EventRelation.builder().eventName("OtherEvent").providerName("removed-provider").consumerName("other").build());
        remaining.addRelation(CommandRelation.builder().commandName("OtherCommand").providerName("other").consumerName("removed-provider").build());
        var kept = EventRelation.builder().eventName("KeptEvent").providerName("other").consumerName("remaining").build();
        remaining.addRelation(kept);
        var model = ArchitectureModel.builder().systems(List.of(remaining, removed)).build();

        model.removeSystem(removed);

        assertThat(model.getSystems()).containsExactly(remaining);
        assertThat(model.getRemovedSystems()).containsExactly(removed);
        assertThat(remaining.getRelations()).containsExactly(kept);
    }

    @Test
    void removalTrackingDoesNotDuplicateMissingEntriesAndNameOnlyLookupNeverAdoptsAnAlias() {
        var team = Team.builder().name("blue").build();
        var system = System.builder().name("foo").aliases(List.of("bar")).build();
        var model = ArchitectureModel.builder().teams(List.of(team)).systems(List.of(system)).build();
        assertThat(model.findSystem("BAR")).contains(system);
        assertThat(model.findSystemByNameIgnoreCase("BAR")).isEmpty();
        assertThat(model.findSystemByNameIgnoreCase("FOO")).contains(system);
        assertThat(model.findTeamByNameIgnoreCase("BLUE")).contains(team);
        model.removeTeam(team);
        model.removeTeam(team);
        model.removeSystem(system);
        model.removeSystem(system);
        assertThat(model.getRemovedTeams()).containsExactly(team);
        assertThat(model.getRemovedSystems()).containsExactly(system);
    }
}
