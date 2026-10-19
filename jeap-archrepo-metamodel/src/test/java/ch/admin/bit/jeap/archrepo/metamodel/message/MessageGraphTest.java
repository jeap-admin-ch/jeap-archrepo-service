package ch.admin.bit.jeap.archrepo.metamodel.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageGraphTest {

    @Test
    void normalizesVariantKeysConsistently() {
        assertThat(MessageGraph.normalizeVariant("OrderEvent", null)).isEmpty();
        assertThat(MessageGraph.normalizeVariant("OrderEvent", "default")).isEmpty();
        assertThat(MessageGraph.normalizeVariant("OrderEvent", "OrderEvent")).isEmpty();
        assertThat(MessageGraph.normalizeVariant("OrderEvent", "OrderEvent/default")).isEmpty();
        assertThat(MessageGraph.normalizeVariant("OrderEvent", "orderevent/DEFAULT")).isEmpty();
        assertThat(MessageGraph.normalizeVariant("OrderEvent", "OrderEvent/priority")).isEqualTo("priority");
        assertThat(MessageGraph.normalizeVariant("OrderEvent", "orderevent/priority")).isEqualTo("priority");
        assertThat(MessageGraph.normalizeVariant("OrderEvent", "priority/high")).isEqualTo("priority/high");
    }
}
