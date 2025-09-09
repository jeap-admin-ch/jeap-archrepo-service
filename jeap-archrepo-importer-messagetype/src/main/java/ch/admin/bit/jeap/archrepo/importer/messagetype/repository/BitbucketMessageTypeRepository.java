package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class BitbucketMessageTypeRepository extends MessageTypeRepository {

    BitbucketMessageTypeRepository(String gitUri) {
        super(gitUri);
    }

    @Override
    protected String processBaseUri(String gitUri) {
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(gitUri).build();
        String projectName = uriComponents.getPathSegments().get(1).toUpperCase();
        String repoName = uriComponents.getPathSegments().get(2).replace(".git", "");
        return UriComponentsBuilder.fromUriString(gitUri)
                .replacePath("projects/%s/repos/%s/".formatted(projectName, repoName))
                .toUriString();
    }
}
