package ch.admin.bit.jeap.archrepo.importer.messagetype.repository.github;

import ch.admin.bit.jeap.archrepo.importer.messagetype.repository.MessageTypeRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class GitHubMessageTypeRepository extends MessageTypeRepository {

    public GitHubMessageTypeRepository(String gitUri, Map<String, String> parameters) {
        super(gitUri);
        if (parameters == null || parameters.isEmpty()) {
            log.info("No GitHub credentials set, will try to check out without credentials");
        } else {
            assertRequiredParameters(parameters);
            setCredentialsProvider(new GitHubAppCredentialsProvider(
                    parameters.get("GITHUB_APP_ID"),
                    parameters.get("GITHUB_PRIVATE_KEY_PEM")
            ));
        }
    }

    // For tests
    GitHubMessageTypeRepository(String gitUri) {
        super(gitUri);
    }

    @Override
    protected String processBaseUri(String gitUri) {
        // From: https://github.com/org/repo.git
        // To: https://github.com/org/repo/blob/-/descriptor/sys/event/myevent/MyEvent_v1.1.0.avdl
        // Note: - is used to address the default branch (main or master usually)
        return gitUri.replace(".git", "/blob/-/descriptor");
    }

    private void assertRequiredParameters(Map<String, String> parameters) {
        if (!parameters.containsKey("GITHUB_APP_ID")) {
            throw new IllegalArgumentException("Missing required parameter 'GITHUB_APP_ID' for GitHub repository");
        }
        if (!parameters.containsKey("GITHUB_PRIVATE_KEY_PEM")) {
            throw new IllegalArgumentException("Missing required parameter 'GITHUB_PRIVATE_KEY_PEM' for GitHub repository");
        }
    }

}
