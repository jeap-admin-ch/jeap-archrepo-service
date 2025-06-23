package ch.admin.bit.jeap.archrepo.metamodel.reaction;

import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReactionStatisticsTest {

    @Test
    void builderShouldInitializeAllFields() {

        SystemComponent component = BackendService.builder()
                .id(UUID.randomUUID())
                .name("TestComponent")
                .build();

        ActionEntity action = ActionEntity.builder().actionType("actionType").actionFqn("com.example.Action").build();
        ReactionStatistics stats = ReactionStatistics.builder()
                .component(component)
                .triggerType("triggerType")
                .triggerFqn("com.example.Trigger")
                .count(100)
                .median(50.0)
                .percentage(75.0)
                .build();
        stats.addAction(action);

        assertThat(stats.getComponent()).isEqualTo(component);
        assertThat(stats.getTriggerType()).isEqualTo("triggerType");
        assertThat(stats.getTriggerFqn()).isEqualTo("com.example.Trigger");
        assertThat(stats.getActions().getFirst().getActionType()).isEqualTo("actionType");
        assertThat(stats.getActions().getFirst().getActionFqn()).isEqualTo("com.example.Action");
        assertThat(stats.getCount()).isEqualTo(100);
        assertThat(stats.getMedian()).isEqualTo(50.0);
        assertThat(stats.getPercentage()).isEqualTo(75.0);
    }

    @Test
    void toStringShouldIncludeRelevantFields() {
        SystemComponent component = BackendService.builder()
                .id(UUID.randomUUID())
                .name("TestComponent")
                .build();
        ActionEntity action = ActionEntity.builder().actionType("actionType").actionFqn("com.example.Action").build();
        ReactionStatistics stats = ReactionStatistics.builder()
                .component(component)
                .triggerType("triggerType")
                .triggerFqn("com.example.Trigger")
                .count(100)
                .median(50.0)
                .percentage(75.0)
                .build();
        stats.addAction(action);

        String toString = stats.toString();
        assertThat(toString).contains("triggerType");
        assertThat(toString).contains("com.example.Trigger");
        assertThat(toString).contains("actionType");
        assertThat(toString).contains("com.example.Action");
        assertThat(toString).doesNotContain("component");
    }

}
