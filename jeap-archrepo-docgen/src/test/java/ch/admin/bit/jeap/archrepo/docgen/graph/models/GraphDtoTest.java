package ch.admin.bit.jeap.archrepo.docgen.graph.models;

    import org.junit.jupiter.api.Test;

    import java.util.List;

    import static org.assertj.core.api.Assertions.assertThat;

    class GraphDtoTest {

        @Test
        void toDot_shouldCreateClustersGroupedByTriggerIdAndComponent() {
            // Arrange
            ReactionNodeDto r1 = new ReactionNodeDto(101, "order-service", false, false);
            ReactionNodeDto r2 = new ReactionNodeDto(102, "order-service", false, false);
            ReactionNodeDto r3 = new ReactionNodeDto(103, "order-service", false, false); // different trigger
            ReactionNodeDto r4 = new ReactionNodeDto(201, "payment-service", false, false);

            MessageNodeDto m1 = new MessageNodeDto(1, "OrderCreatedEvent", null, false);
            MessageNodeDto m2 = new MessageNodeDto(2, "OrderCancelledEvent", null, false);
            MessageNodeDto m3 = new MessageNodeDto(3, "PaymentReceivedCommand", null, false);

            TriggerEdgeDto e1 = new TriggerEdgeDto(1, NodeDtoType.MESSAGE, 101, 10);
            TriggerEdgeDto e2 = new TriggerEdgeDto(1, NodeDtoType.MESSAGE, 102, 20);
            TriggerEdgeDto e3 = new TriggerEdgeDto(2, NodeDtoType.MESSAGE, 103, 15);
            TriggerEdgeDto e4 = new TriggerEdgeDto(3, NodeDtoType.MESSAGE, 201, 5);

            GraphDto graph = new GraphDto(
                    List.of(r1, r2, r3, r4, m1, m2, m3),
                    List.of(e1, e2, e3, e4)
            );

            // Act
            String dot = graph.toDot();

            // Assert: Cluster for trigger ID 1 and component "order" exists
            assertThat(dot).contains("subgraph \"cluster_trigger_1_order\"");
            assertThat(dot).contains("label=\"order\"");
            assertThat(r1.isPartOfCluster()).isTrue();
            assertThat(r2.isPartOfCluster()).isTrue();

            // Assert: For trigger ID 2 and component "order" there is only one reaction → no cluster
            assertThat(dot).doesNotContain("cluster_trigger_2_order");
            assertThat(r3.isPartOfCluster()).isFalse();

            // Assert: "payment-service" has only 1 reaction → no cluster
            assertThat(dot).doesNotContain("cluster_trigger_3_payment");
            assertThat(dot).contains("\"REACTION-201\" [label=\"payment\\n201\"");
            assertThat(r4.isPartOfCluster()).isFalse();

            // Assert: Edges are correctly included
            assertThat(dot).contains("\"MESSAGE-1\" -> \"REACTION-101\"");
            assertThat(dot).contains("\"MESSAGE-1\" -> \"REACTION-102\"");
            assertThat(dot).contains("\"MESSAGE-2\" -> \"REACTION-103\"");
            assertThat(dot).contains("\"MESSAGE-3\" -> \"REACTION-201\"");
        }

        @Test
        void toDot_shouldNotCreateClustersWhenAllReactionsAreSingle() {
            ReactionNodeDto r1 = new ReactionNodeDto(101, "order-service", false, false);
            ReactionNodeDto r2 = new ReactionNodeDto(201, "payment-service", false, false);

            MessageNodeDto m1 = new MessageNodeDto(1, "OrderCreatedEvent", null, false);
            MessageNodeDto m2 = new MessageNodeDto(2, "PaymentReceivedCommand", null, false);

            TriggerEdgeDto e1 = new TriggerEdgeDto(1, NodeDtoType.MESSAGE, 101, 10);
            TriggerEdgeDto e2 = new TriggerEdgeDto(2, NodeDtoType.MESSAGE, 201, 5);

            GraphDto graph = new GraphDto(List.of(r1, r2, m1, m2), List.of(e1, e2));

            String dot = graph.toDot();

            // Assert: No clusters present
            assertThat(dot).doesNotContain("subgraph \"cluster_trigger");
            assertThat(r1.isPartOfCluster()).isFalse();
            assertThat(r2.isPartOfCluster()).isFalse();
        }

        @Test
        void toDot_shouldNotCreateClustersWhenEachComponentHasOnlyOneReactionUnderSameTrigger() {
            // Arrange
            ReactionNodeDto r1 = new ReactionNodeDto(101, "order-service", false, false);
            ReactionNodeDto r2 = new ReactionNodeDto(201, "payment-service", false, false);

            MessageNodeDto m1 = new MessageNodeDto(1, "OrderCreatedEvent", null, false);

            TriggerEdgeDto e1 = new TriggerEdgeDto(1, NodeDtoType.MESSAGE, 101, 10);
            TriggerEdgeDto e2 = new TriggerEdgeDto(1, NodeDtoType.MESSAGE, 201, 20);

            GraphDto graph = new GraphDto(List.of(r1, r2, m1), List.of(e1, e2));

            // Act
            String dot = graph.toDot();

            // Assert: For trigger ID 1 and component "order" there is only one reaction → no cluster
            assertThat(dot).doesNotContain("subgraph \"cluster_trigger_1_order\"");
            assertThat(r1.isPartOfCluster()).isFalse();

            // Assert: For trigger ID 1 and component "payment" there is only one reaction → no cluster
            assertThat(dot).doesNotContain("subgraph \"cluster_trigger_1_payment\"");
            assertThat(r2.isPartOfCluster()).isFalse();
        }

        @Test
        void toDot_shouldIncludeReactionsWithoutTriggerOutsideClusters() {
            // Arrange: Reactions without any trigger
            ReactionNodeDto r1 = new ReactionNodeDto(101, "orphan-service", false, false);
            ReactionNodeDto r2 = new ReactionNodeDto(102, "standalone-service", false, false);

            // Arrange: Message and edges (empty, so no triggers)
            MessageNodeDto m1 = new MessageNodeDto(1, "DummyEvent", null, false);

            GraphDto graph = new GraphDto(List.of(r1, r2, m1), List.of());

            // Act
            String dot = graph.toDot();

            // Assert: Both reactions should appear in DOT
            assertThat(dot).contains("\"REACTION-101\" [label=\"orphan\\n101\"");
            assertThat(dot).contains("\"REACTION-102\" [label=\"standalone\\n102\"");

            // Assert: No clusters created
            assertThat(dot).doesNotContain("subgraph \"cluster_trigger");
            assertThat(r1.isPartOfCluster()).isFalse();
            assertThat(r2.isPartOfCluster()).isFalse();
        }
    }