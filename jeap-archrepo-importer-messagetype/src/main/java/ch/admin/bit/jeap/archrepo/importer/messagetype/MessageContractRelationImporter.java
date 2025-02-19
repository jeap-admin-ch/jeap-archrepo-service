package ch.admin.bit.jeap.archrepo.importer.messagetype;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageContract;
import ch.admin.bit.jeap.archrepo.metamodel.relation.CommandRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageContractRelationImporter {

    public static void createRelations(ArchitectureModel architectureModel) {
        List<System> systems = architectureModel.getSystems();

        systems.stream()
                .flatMap(system -> system.getEvents().stream())
                .forEach(event -> addEventRelations(architectureModel, event));

        systems.stream()
                .flatMap(system -> system.getCommands().stream())
                .forEach(command -> addCommandRelations(architectureModel, command));
    }

    private static void addEventRelations(ArchitectureModel architectureModel, Event event) {
        for (MessageContract publisherContract : event.getPublisherContracts()) {
            Optional<SystemComponent> publisher = getComponent(architectureModel, publisherContract);
            boolean hasKnownSubscribers = false;
            for (MessageContract subscriberContract : event.getConsumerContracts()) {
                if ((publisherContract.getTopic() != null) && publisherContract.getTopic().equals(subscriberContract.getTopic())) {
                    Optional<SystemComponent> subscriber = getComponent(architectureModel, subscriberContract);
                    if (publisher.isPresent() || subscriber.isPresent()) {
                        addEventRelation(event, publisher, subscriber);
                        hasKnownSubscribers = true;
                    }
                }
            }
            if (!hasKnownSubscribers && publisher.isPresent()) {
                addEventRelation(event, publisher, Optional.empty());
            }
        }
    }

    private static void addEventRelation(Event event, Optional<SystemComponent> publisher, Optional<SystemComponent> subscriber) {
        String providerName = publisher.map(SystemComponent::getName).orElse(null);
        String consumerName = subscriber.map(SystemComponent::getName).orElse(null);
        EventRelation relation = EventRelation.builder()
                .providerName(providerName)
                .consumerName(consumerName)
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .eventName(event.getMessageTypeName())
                .build();

        System system = event.getParent();
        if (!system.getRelations().contains(relation)) {
            log.info("Adding relation for event {} from {} to {}", event.getMessageTypeName(), providerName, consumerName);
            system.addRelation(relation);
        } else {
            log.info("Relation exists for event {} from {} to {}", event.getMessageTypeName(), providerName, consumerName);
        }
    }

    private static void addCommandRelations(ArchitectureModel architectureModel, Command command) {
        for (MessageContract receiverContract : command.getReceiverContracts()) {
            Optional<SystemComponent> receiver = getComponent(architectureModel, receiverContract);
            boolean hasKnownSenders = false;
            for (MessageContract senderContract : command.getSenderContracts()) {
                if ((receiverContract.getTopic() != null) && receiverContract.getTopic().equals(senderContract.getTopic())) {
                    Optional<SystemComponent> sender = getComponent(architectureModel, senderContract);
                    if (receiver.isPresent() || sender.isPresent()) {
                        addCommandRelation(command, receiver, sender);
                        hasKnownSenders = true;
                    }
                }
            }
            if (!hasKnownSenders && receiver.isPresent()) {
                addCommandRelation(command, receiver, Optional.empty());
            }
        }
    }

    private static void addCommandRelation(Command command, Optional<SystemComponent> receiver, Optional<SystemComponent> sender) {
        String providerName = sender.map(SystemComponent::getName).orElse(null);
        String consumerName = receiver.map(SystemComponent::getName).orElse(null);
        CommandRelation relation = CommandRelation.builder()
                .providerName(providerName)
                .consumerName(consumerName)
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .commandName(command.getMessageTypeName())
                .build();

        System system = command.getParent();
        if (!system.getRelations().contains(relation)) {
            log.info("Adding relation for command {} from {} to {}", command.getMessageTypeName(), providerName, consumerName);
            system.addRelation(relation);
        } else {
            log.info("Relation exists for command {} from {} to {}", command.getMessageTypeName(), providerName, consumerName);
        }
    }

    private static Optional<SystemComponent> getComponent(ArchitectureModel architectureModel, MessageContract messageContract) {
        return architectureModel.findSystemComponent(messageContract.getComponentName());
    }
}
