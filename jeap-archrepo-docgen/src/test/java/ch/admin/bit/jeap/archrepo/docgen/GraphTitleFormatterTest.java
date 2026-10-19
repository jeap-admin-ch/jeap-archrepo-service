package ch.admin.bit.jeap.archrepo.docgen;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GraphTitleFormatterTest {

    @Test
    void formatsGraphTitlesWithNames() {
        assertThat(GraphTitleFormatter.systemGraph("WVS"))
                .isEqualTo("System Graph - WVS");
        assertThat(GraphTitleFormatter.serviceGraph("wvs-communication-service"))
                .isEqualTo("Service Graph - wvs-communication-service");
        assertThat(GraphTitleFormatter.messageGraph("WvsMessageValidated", "NC123"))
                .isEqualTo("Message Graph - WvsMessageValidated - NC123");
    }

    @Test
    void omitsMissingNamesWithoutDanglingSeparators() {
        assertThat(GraphTitleFormatter.systemGraph(null)).isEqualTo("System Graph");
        assertThat(GraphTitleFormatter.serviceGraph("")).isEqualTo("Service Graph");
        assertThat(GraphTitleFormatter.messageGraph("  ", null)).isEqualTo("Message Graph");
    }

    @Test
    void omitsMissingAndDefaultMessageVariants() {
        assertThat(GraphTitleFormatter.messageGraph("WvsMessageValidated", null))
                .isEqualTo("Message Graph - WvsMessageValidated");
        assertThat(GraphTitleFormatter.messageGraph("WvsMessageValidated", ""))
                .isEqualTo("Message Graph - WvsMessageValidated");
        assertThat(GraphTitleFormatter.messageGraph("WvsMessageValidated", "  "))
                .isEqualTo("Message Graph - WvsMessageValidated");
        assertThat(GraphTitleFormatter.messageGraph("WvsMessageValidated", "Default"))
                .isEqualTo("Message Graph - WvsMessageValidated");
    }

    @Test
    void trimsTitleParts() {
        assertThat(GraphTitleFormatter.messageGraph(" WvsMessageValidated ", " NC123 "))
                .isEqualTo("Message Graph - WvsMessageValidated - NC123");
    }
}
