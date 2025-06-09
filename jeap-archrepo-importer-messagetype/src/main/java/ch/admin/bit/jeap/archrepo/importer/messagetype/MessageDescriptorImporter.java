package ch.admin.bit.jeap.archrepo.importer.messagetype;

import ch.admin.bit.jeap.archrepo.importer.messagetype.repository.*;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RequiredArgsConstructor
@Slf4j
public class MessageDescriptorImporter {

    private final MessageTypeRepositoryFactory messageTypeRepositoryFactory;

    public void importDescriptors(ArchitectureModel architectureModel) {
        List<MessageTypeRepository> messageTypeRepositories = messageTypeRepositoryFactory.cloneRepositories();
        try {
            for (MessageTypeRepository messageTypeRepository : messageTypeRepositories) {
                messageTypeRepository.getAllEventDescriptors().forEach(event ->
                        importEvent(architectureModel, messageTypeRepository, event));
                messageTypeRepository.getAllCommandDescriptors().forEach(command ->
                        importCommand(architectureModel, messageTypeRepository, command));
            }
        } finally {
            messageTypeRepositories.forEach(MessageTypeRepository::close);
        }
    }

    private void importEvent(ArchitectureModel architectureModel, MessageTypeRepository messageTypeRepository, EventDescriptor eventDescriptor) {
        Optional<System> definingSystem = architectureModel.findSystem(eventDescriptor.getDefiningSystem());
        if (definingSystem.isEmpty()) {
            log.warn("Event {} is defined by {} but this system is not known, ignore it",
                    kv("messageType", eventDescriptor.getMessageTypeName()), kv("unknownSystem", eventDescriptor.getDefiningSystem()));
            return;
        }

        Event event = createEvent(eventDescriptor, messageTypeRepository);
        definingSystem.get().addEvent(event);
        log.info("Importing {}", event.getMessageTypeName());
    }

    private Event createEvent(EventDescriptor eventDescriptor, MessageTypeRepository messageTypeRepository) {
        List<MessageVersion> messageVersions = getMessageVersions(eventDescriptor, messageTypeRepository);

        return Event.builder()
                .id(UUID.randomUUID())
                .messageTypeName(eventDescriptor.getMessageTypeName())
                .topic(eventDescriptor.getTopic())
                .description(eventDescriptor.getDescription())
                .documentationUrl(eventDescriptor.getDocumentationUrl())
                .descriptorUrl(messageTypeRepository.getDescriptorUrl(eventDescriptor))
                .scope(eventDescriptor.getScope())
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .messageVersions(messageVersions)
                .build();
    }

    private void importCommand(ArchitectureModel architectureModel, MessageTypeRepository messageTypeRepository, CommandDescriptor commandDescriptor) {
        Optional<System> definingSystem = architectureModel.findSystem(commandDescriptor.getDefiningSystem());
        if (definingSystem.isEmpty()) {
            log.warn("Command {} is defined by {} but this system is not known, ignore it",
                    kv("messageType", commandDescriptor.getMessageTypeName()), kv("unknownSystem", commandDescriptor.getDefiningSystem()));
            return;
        }

        Command command = createCommand(commandDescriptor, messageTypeRepository);
        definingSystem.get().addCommand(command);
        log.info("Importing {}", command.getMessageTypeName());
    }

    private Command createCommand(CommandDescriptor commandDescriptor, MessageTypeRepository messageTypeRepository) {
        List<MessageVersion> messageVersions = getMessageVersions(commandDescriptor, messageTypeRepository);

        return Command.builder()
                .id(UUID.randomUUID())
                .messageTypeName(commandDescriptor.getMessageTypeName())
                .topic(commandDescriptor.getTopic())
                .description(commandDescriptor.getDescription())
                .documentationUrl(commandDescriptor.getDocumentationUrl())
                .descriptorUrl(messageTypeRepository.getDescriptorUrl(commandDescriptor))
                .scope(commandDescriptor.getScope())
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .messageVersions(messageVersions)
                .build();
    }

    private List<MessageVersion> getMessageVersions(MessageTypeDescriptor messageTypeDescriptor, MessageTypeRepository messageTypeRepository) {
        return messageTypeDescriptor.getVersions().stream()
                .map(v -> MessageVersion.builder()
                        .keySchemaName(v.getKeySchema())
                        .valueSchemaName(v.getValueSchema())
                        .keySchemaUrl(v.getKeySchema() == null ? null : messageTypeRepository.getSchemaUrl(messageTypeDescriptor, v.getKeySchema()))
                        .keySchemaResolved(v.getKeySchema() == null ? null : SchemaImportResolver.resolveImportsFromSchema(messageTypeRepository.getSchemaFile(messageTypeDescriptor, v.getKeySchema())))
                        .valueSchemaUrl(messageTypeRepository.getSchemaUrl(messageTypeDescriptor, v.getValueSchema()))
                        .valueSchemaResolved(SchemaImportResolver.resolveImportsFromSchema(messageTypeRepository.getSchemaFile(messageTypeDescriptor, v.getValueSchema())))
                        .version(v.getVersion())
                        .compatibilityMode(v.getCompatibilityMode())
                        .compatibleVersion(compatibleVersion(messageTypeDescriptor.getVersions(), v))
                        .build())
                .collect(Collectors.toList());
    }

    private String compatibleVersion(List<MessageTypeVersion> versions, MessageTypeVersion version) {
        if (version.getCompatibilityMode() == null) {
            return null;
        }

        if (version.getCompatibleVersion() != null) {
            return version.getCompatibleVersion();
        }

        List<SemanticVersion> sortedVersions = versions.stream()
                .map(MessageTypeVersion::semanticVersion)
                .sorted()
                .toList();

        int versionIndex = sortedVersions.indexOf(version.semanticVersion());
        int predecessorVersion = versionIndex - 1;
        if (predecessorVersion >= 0) {
            return sortedVersions.get(predecessorVersion).toString();
        }
        return null;
    }
}
