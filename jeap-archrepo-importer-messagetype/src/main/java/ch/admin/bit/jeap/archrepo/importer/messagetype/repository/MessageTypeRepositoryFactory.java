package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import ch.admin.bit.jeap.archrepo.importer.messagetype.MessageTypeImporterProperties;
import ch.admin.bit.jeap.archrepo.importer.messagetype.RepositoryProperties;
import ch.admin.bit.jeap.archrepo.importer.messagetype.repository.github.GitHubMessageTypeRepository;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageTypeRepositoryFactory {

    @Getter
    private final List<MessageTypeRepository> repositories;

    public MessageTypeRepositoryFactory(MessageTypeImporterProperties messageTypeImporterProperties) {
        List<RepositoryProperties> repositoryPropertiesList = messageTypeImporterProperties.getRepositories();
        if (repositoryPropertiesList == null || repositoryPropertiesList.isEmpty()) {
            // Fallback to deprecated gitUris property
            repositoryPropertiesList = messageTypeImporterProperties.getGitUris().stream()
                    .map(uri -> new RepositoryProperties(uri, RepositoryProperties.RepositoryType.BITBUCKET))
                    .toList();
        }
        repositories = repositoryPropertiesList.stream()
                .map(repository -> {
                    switch (repository.getType()) {
                        case LOCAL -> {
                            return new LocalFolderMessageTypeRepository(repository.getUri());
                        }
                        case GITHUB -> {
                            return new GitHubMessageTypeRepository(repository.getUri(), repository.getParameters());
                        }
                        default -> {
                            return new BitbucketMessageTypeRepository(repository.getUri());
                        }
                    }
                })
                .toList();
    }

    public List<MessageTypeRepository> cloneRepositories() {
        repositories.forEach(MessageTypeRepository::cloneGitRepo);
        return repositories;
    }

}
