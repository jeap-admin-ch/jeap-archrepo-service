package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import ch.admin.bit.jeap.archrepo.importer.messagetype.MessageTypeImporterProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MessageTypeRepositoryFactory {

    private final MessageTypeImporterProperties messageTypeImporterProperties;
    private final List<UrlBasedCredentialsProvider> urlBasedCredentialsProviders = new ArrayList<>();

    @Autowired
    public MessageTypeRepositoryFactory(MessageTypeImporterProperties messageTypeImporterProperties, List<UrlBasedCredentialsProvider> urlBasedCredentialsProviders) {
        this.messageTypeImporterProperties = messageTypeImporterProperties;
        this.urlBasedCredentialsProviders.addAll(urlBasedCredentialsProviders);
    }

    public List<MessageTypeRepository> cloneRepositories() {
        return messageTypeImporterProperties.getGitUris().stream()
                .map(uri -> {
                    UrlBasedCredentialsProvider provider = urlBasedCredentialsProviders.stream()
                            .filter(p -> p.supports(uri))
                            .findFirst()
                            .orElse(null);
                    return new MessageTypeRepository(uri, provider);
                })
                .toList();
    }

}
