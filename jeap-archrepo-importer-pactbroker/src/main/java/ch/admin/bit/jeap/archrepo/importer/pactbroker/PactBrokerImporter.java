package ch.admin.bit.jeap.archrepo.importer.pactbroker;

import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.core.model.RequestResponseInteraction;
import ch.admin.bit.jeap.archrepo.importer.pactbroker.client.BrokeredPact;
import ch.admin.bit.jeap.archrepo.importer.pactbroker.client.PactBrokerClient;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModelHelper;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SelfContainedSystem;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
class PactBrokerImporter implements ArchRepoImporter {

    private static final Pattern NAME_PARTS = Pattern.compile("(?<departement>.+?)-(?<system>.+?)-(?<component>.+)");

    private final PactBrokerClient pactBrokerClient;

    @Override
    public void importIntoModel(ArchitectureModel model, String environment) {
        // Full import, remove and re-import all Pact relations
        model.removeAllByImporter(Importer.PACT_BROKER);

        pactBrokerClient.withAllPacts(pact -> importRelationsFromPact(model, pact));
    }

    private void importRelationsFromPact(ArchitectureModel model, BrokeredPact brokeredPact) {
        Pact pact = brokeredPact.getPact();
        Optional<SystemComponent> consumerComponent = ArchitectureModelHelper.findComponentByNameWithSystemPrefix(model, pact.getConsumer().getName(), NAME_PARTS);
        Optional<SystemComponent> providerComponent = ArchitectureModelHelper.findComponentByNameWithSystemPrefix(model, removeSuffix(pact.getProvider().getName()), NAME_PARTS);

        if (consumerComponent.isEmpty()) {
            log.warn("Consumer {} not found", pact.getConsumer().getName());
            return;
        }
        if (providerComponent.isEmpty()) {
            log.warn("Provider {} not found", pact.getProvider().getName());
            return;
        }
        if (!(providerComponent.get() instanceof BackendService) && !(providerComponent.get() instanceof SelfContainedSystem)) {
            log.warn("Provider {} found, but is not a component type that can provide REST APIs", pact.getProvider().getName());
            return;
        }

        String pactUrl = brokeredPact.getPactUrl();
        RequestResponseInteractionImporter requestResponseImporter =
                new RequestResponseInteractionImporter(consumerComponent.get(), providerComponent.get(), pactUrl);
        pact.getInteractions().forEach(interaction -> importInteraction(interaction, requestResponseImporter));
    }

    private void importInteraction(Interaction interaction, RequestResponseInteractionImporter requestResponseImporter) {
        if (interaction instanceof RequestResponseInteraction responseInteraction) {
            requestResponseImporter.importInteraction(responseInteraction);
        } else {
            log.warn("Unsupported interaction type {}", interaction.getClass().getName());
        }
    }
    private static String removeSuffix(String input) {
        int lastIndex = input.lastIndexOf("_");
        if (lastIndex != -1) {
            return input.substring(0, lastIndex);
        } else return input;
    }
}
