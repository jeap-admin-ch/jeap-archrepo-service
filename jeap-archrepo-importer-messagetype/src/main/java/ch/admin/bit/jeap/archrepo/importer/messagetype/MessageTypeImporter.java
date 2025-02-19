package ch.admin.bit.jeap.archrepo.importer.messagetype;

import ch.admin.bit.jeap.archrepo.importer.messagetype.repository.MessageTypeRepositoryFactory;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
class MessageTypeImporter implements ArchRepoImporter {

    private final MessageTypeRepositoryFactory messageTypeRepositoryFactory;
    private final MessageContractImporter messageContractImporter;

    @Override
    public void importIntoModel(ArchitectureModel architectureModel) {
        //Replace all events/commands and event/command relations
        architectureModel.removeAllByImporter(Importer.MESSAGE_TYPE_REGISTRY);

        MessageDescriptorImporter messageDescriptorImporter = new MessageDescriptorImporter(messageTypeRepositoryFactory);
        messageDescriptorImporter.importDescriptors(architectureModel);

        messageContractImporter.importMessageContracts(architectureModel);

        MessageContractRelationImporter.createRelations(architectureModel);
    }
}
