package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.reaction.Action;
import ch.admin.bit.jeap.archrepo.metamodel.reaction.ReactionStatistics;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class ReactionStatisticsRepositoryTest {

    @Autowired
    private ReactionStatisticsRepository repository;

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void findByDefiningSystemAndComponent() {
        Team team = teamRepository.save(Team.builder().name("team").build());

        System systemA = System.builder()
                .name("systemA")
                .defaultOwner(team)
                .build();
        System systemB = System.builder()
                .name("systemB")
                .defaultOwner(team)
                .build();
        BackendService componentA = BackendService.builder()
                .name("componentA")
                .parent(systemA)
                .id(UUID.randomUUID())
                .ownedBy(team)
                .createdAt(ZonedDateTime.now())
                .build();
        systemA.addSystemComponent(componentA);

        BackendService componentB = BackendService.builder()
                .name("componentB")
                .parent(systemA)
                .id(UUID.randomUUID())
                .ownedBy(team)
                .createdAt(ZonedDateTime.now())
                .build();
        systemA.addSystemComponent(componentB);

        systemRepository.save(systemA);

        BackendService componentC = BackendService.builder()
                .name("componentC")
                .parent(systemB)
                .id(UUID.randomUUID())
                .ownedBy(team)
                .createdAt(ZonedDateTime.now())
                .build();
        systemB.addSystemComponent(componentC);
        systemRepository.save(systemB);

        Action action = Action.builder().actionType("actionType1")
                .actionFqn("com.example.ActionA")
                .build();
        ReactionStatistics statistics = ReactionStatistics.builder()
                .component(componentA)
                .triggerType("triggerType1")
                .triggerFqn("com.example.TriggerA")
                .count(10)
                .median(5.0)
                .percentage(50.0)
                .build();
        statistics.addAction(action);
        repository.save(statistics);

        Action action1 = Action.builder().actionType("actionType1")
                .actionFqn("com.example.ActionB")
                .build();
        ReactionStatistics statistics1 = ReactionStatistics.builder()
                .component(componentA)
                .triggerType("triggerType1")
                .triggerFqn("com.example.TriggerB")
                .count(20)
                .median(10.0)
                .percentage(60.0)
                .build();
        statistics1.addAction(action1);
        repository.save(statistics1);

        Action action2 = Action.builder()
                .actionType("actionType1")
                .actionFqn("com.example.ActionC")
                .build();
        ReactionStatistics statistics2 = ReactionStatistics.builder()
                .component(componentB)
                .triggerType("triggerType1")
                .triggerFqn("com.example.TriggerC")
                .count(30)
                .median(15.0)
                .percentage(70.0)
                .build();
        statistics2.addAction(action2);
        repository.save(statistics2);

        Action action3 = Action.builder()
                .actionType("actionType1")
                .actionFqn("com.example.ActionD")
                .build();
        ReactionStatistics statistics3 = ReactionStatistics.builder()
                .component(componentC)
                .triggerType("triggerType1")
                .triggerFqn("com.example.TriggerD")
                .count(40)
                .median(20.0)
                .percentage(80.0)
                .build();
        statistics3.addAction(action3);
        repository.save(statistics3);

        List<ReactionStatistics> componentAStats = repository.findByComponent(componentA);
        assertThat(componentAStats).hasSize(2);

        List<ReactionStatistics> componentBStats = repository.findByComponent(componentB);
        assertThat(componentBStats).hasSize(1);

        List<ReactionStatistics> componentCStats = repository.findByComponent(componentC);
        assertThat(componentCStats).hasSize(1);
        assertThat(componentCStats.getFirst().getTriggerType()).isEqualTo("triggerType1");
        assertThat(componentCStats.getFirst().getTriggerFqn()).isEqualTo("com.example.TriggerD");
        assertThat(componentCStats.getFirst().getActions().getFirst().getActionType()).isEqualTo("actionType1");
        assertThat(componentCStats.getFirst().getActions().getFirst().getActionFqn()).isEqualTo("com.example.ActionD");
        assertThat(componentCStats.getFirst().getCount()).isEqualTo(40);
        assertThat(componentCStats.getFirst().getMedian()).isEqualTo(20.0);
        assertThat(componentCStats.getFirst().getPercentage()).isEqualTo(80.0);
    }
}
