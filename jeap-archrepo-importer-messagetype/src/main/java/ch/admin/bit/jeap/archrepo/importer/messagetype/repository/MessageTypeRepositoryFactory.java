package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import ch.admin.bit.jeap.archrepo.importer.messagetype.MessageTypeImporterProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageTypeRepositoryFactory {

    private final MessageTypeImporterProperties messageTypeImporterProperties;

    public List<MessageTypeRepository> cloneRepositories() {
        return messageTypeImporterProperties.getGitUris().stream()
                .map(MessageTypeRepository::new)
                .toList();
    }
}
