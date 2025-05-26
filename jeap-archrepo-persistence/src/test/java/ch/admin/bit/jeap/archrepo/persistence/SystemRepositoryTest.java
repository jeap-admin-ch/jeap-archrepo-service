package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageContract;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageVersion;
import ch.admin.bit.jeap.archrepo.metamodel.reaction.ReactionStatistics;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class SystemRepositoryTest {

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void save_and_findSystem() {
        Team team = teamRepository.save(Team.builder().name("team").build());

        System system = System.builder().name("MySystem").defaultOwner(team).build();
        SystemComponent systemComponent = BackendService.builder()
                .id(UUID.randomUUID())
                .name("mySystem-billing-service")
                .ownedBy(team)
                .build();
        ReflectionTestUtils.setField(systemComponent, "createdAt", ZonedDateTime.now());

        system.addSystemComponent(systemComponent);

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .messageTypeName("TestEvent")
                .description("desc")
                .documentationUrl("url")
                .topic("topic")
                .descriptorUrl("link")
                .publisherContracts(List.of(MessageContract.builder().componentName("publisherService").topic("topic").version(List.of("1.2.0")).build()))
                .consumerContracts(List.of(MessageContract.builder().componentName("subscriberService").topic("topic").version(List.of("1.1.0", "1.2.0")).build()))
                .messageVersions(List.of(
                        MessageVersion.builder()
                                .version("1.1.0")
                                .valueSchemaName("value")
                                .valueSchemaUrl("linkS")
                                .valueSchemaResolved("Value Schema 1.1.0 Resolved with \"special\" <character> ...")
                                .build(),
                        MessageVersion.builder()
                                .version("1.2.0")
                                .keySchemaName("key")
                                .keySchemaUrl("linkS")
                                .valueSchemaName("value2")
                                .valueSchemaUrl("linkS")
                                .keySchemaResolved("Key Schema 1.2.0 Resolved with \"special\" <character> ...")
                                .valueSchemaResolved("Value Schema 1.2.0 Resolved with \"special\" <character> ...")
                                .compatibleVersion("1.1.0")
                                .compatibilityMode("BACKWARD")
                                .build()))
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .scope("public")
                .build();
        system.addEvent(event);

        System savedSystem = systemRepository.save(system);

        ReactionStatistics statistics = ReactionStatistics.builder()
                .component(savedSystem.getSystemComponents().getFirst())
                .triggerType("triggerType1")
                .triggerFqn("com.example.TriggerD")
                .actionType("actionType1")
                .actionFqn("com.example.ActionD")
                .count(40)
                .median(20.0)
                .percentage(80.0)
                .build();
        savedSystem.getSystemComponents().getFirst().setReactionStatistics(statistics);

        Optional<System> optionalSystem = systemRepository.findByNameContainingIgnoreCase("mySystem");

        assertThat(optionalSystem)
                .isPresent()
                .contains(savedSystem);

        SystemComponent first = savedSystem.getSystemComponents().getFirst();
        ReactionStatistics reactionStatistics = first.getReactionStatistics();
        assertThat(reactionStatistics).isNotNull();
        assertThat(reactionStatistics.getCount()).isEqualTo(40);
    }


}

