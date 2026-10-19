package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UndeployedRelationsContextTest {

    private BackendService producer;
    private BackendService consumer;
    private System producerSystem;
    private System consumerSystem;
    private ArchitectureModel model;

    @BeforeEach
    void setUp() {
        producer = BackendService.builder().name("orders-producer-service").build();
        consumer = BackendService.builder().name("billing-consumer-service").build();
        producerSystem = System.builder().name("orders").systemComponents(List.of(producer)).build();
        consumerSystem = System.builder().name("billing").systemComponents(List.of(consumer)).build();
        producerSystem.addRelation(EventRelation.builder()
                .eventName("OrderPlacedEvent")
                .providerName(producer.getName())
                .consumerName(consumer.getName())
                .build());
        model = ArchitectureModel.builder().systems(List.of(producerSystem, consumerSystem)).build();
    }

    @Test
    void systemContext_listsTheRelationWhileBothEndsAreDeployed() {
        assertThat(SystemContext.of(model, producerSystem).getProducedEventRelations()).hasSize(1);
        assertThat(SystemContext.of(model, consumerSystem).getConsumedEventRelations()).hasSize(1);
    }

    @Test
    void systemContext_leavesOutARelationWithAnUndeployedEnd() {
        consumer.markUndeployed(ZonedDateTime.now());

        assertThat(SystemContext.of(model, producerSystem).getProducedEventRelations()).isEmpty();
        assertThat(SystemContext.of(model, consumerSystem).getConsumedEventRelations()).isEmpty();
    }

    @Test
    void componentContext_listsTheRelationWhileBothEndsAreDeployed() {
        assertThat(ComponentContext.of(model, producer).getProducedEventRelations()).hasSize(1);
        assertThat(ComponentContext.of(model, consumer).getConsumedEventRelations()).hasSize(1);
    }

    @Test
    void componentContext_leavesOutARelationWithAnUndeployedEnd() {
        producer.markUndeployed(ZonedDateTime.now());

        assertThat(ComponentContext.of(model, producer).getProducedEventRelations()).isEmpty();
        assertThat(ComponentContext.of(model, consumer).getConsumedEventRelations()).isEmpty();
    }
}
