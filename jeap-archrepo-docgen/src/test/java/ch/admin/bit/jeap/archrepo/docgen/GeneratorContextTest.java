package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.GeneratorContext.PageRef;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.ReactionNodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratorContextTest {

    @Test
    void resolvesMessageAndComponentPageUrlsCaseInsensitively() {
        GeneratorContext context = new GeneratorContext(ArchitectureModel.builder().build(), "root", "https://confluence/");
        context.getMessagePages().put("orderevent", new PageRef("101", "events", "OrderEvent"));
        context.getComponentPages().put("orders-service", new PageRef("202", "components", "orders-service"));

        assertThat(context.resolveNodeUrl(new MessageNodeDto(1, "OrderEvent", null, false)))
                .isEqualTo("https://confluence/pages/viewpage.action?pageId=101");
        assertThat(context.resolveNodeUrl(new ReactionNodeDto(2, "ORDERS-SERVICE", false, false)))
                .isEqualTo("https://confluence/pages/viewpage.action?pageId=202");
    }

    @Test
    void doesNotLinkAmbiguousMessageNames() {
        GeneratorContext context = new GeneratorContext(ArchitectureModel.builder().build(), "root", "https://confluence");
        context.addMessagePage("StatusEvent", new PageRef("101", "system-a-events", "StatusEvent"));
        context.addMessagePage("statusevent", new PageRef("202", "system-b-events", "StatusEvent"));

        assertThat(context.resolveNodeUrl(new MessageNodeDto(1, "StatusEvent", null, false))).isNull();
    }
}
