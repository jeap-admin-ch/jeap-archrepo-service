package ch.admin.bit.jeap.archrepo.importer.pactbroker.client;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.RequestResponseInteraction;
import ch.admin.bit.jeap.archrepo.importer.pactbroker.PactStubBrokerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(PactStubBrokerExtension.class)
class PactBrokerClientTest {

    @Test
    void loadPacts(PactBrokerClient client) {
        List<BrokeredPact> pacts = new ArrayList<>();
        client.withAllPacts(pacts::add);

        assertEquals(6, pacts.size());
        BrokeredPact pact = pacts.getFirst();
        assertEquals("bit-jeap-error-handling-service", pact.getPact().getConsumer().getName());
        assertEquals("ezv-shared-agir-service", pact.getPact().getProvider().getName());
        assertEquals(5, pact.getPact().getInteractions().size());
        Interaction interaction = pact.getPact().getInteractions().getFirst();
        assertInstanceOf(RequestResponseInteraction.class, interaction);
        assertEquals("a request to create a new task of an existing task type", interaction.getDescription());

        BrokeredPact anotherPact = pacts.get(4);
        assertEquals("bit-jme-cdc-segregatedProvider-service_moduleA", anotherPact.getPact().getProvider().getName());
    }
}
