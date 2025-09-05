package ch.admin.bit.jeap.archrepo.importer.messagetype;

import ch.admin.bit.jeap.archrepo.importer.messagetype.repository.MessageTypeRepositoryFactory;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageDescriptorImporterIT extends CreateLocalGitRepoBaseTest {


    @Test
    void importMessageTypes() {

        MessageTypeImporterProperties props = new MessageTypeImporterProperties(
                List.of(repoUrl),
                null,
                List.of(new RepositoryProperties(repoUrl, RepositoryProperties.RepositoryType.BITBUCKET)));

        MessageTypeRepositoryFactory factory = new MessageTypeRepositoryFactory(props);
        MessageDescriptorImporter importer = new MessageDescriptorImporter(factory);

        ArchitectureModel model = ArchitectureModel.builder()
                .systems(List.of(
                        System.builder().name("_SHARED").build()))
                .build();

        importer.importDescriptors(model);

        assertThat(model.getAllMessageTypes())
                .anyMatch(mt -> mt.getMessageTypeName().equals("SharedArchivedArtifactVersionCreatedEvent"));
    }
}
