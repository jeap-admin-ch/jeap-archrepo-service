package ch.admin.bit.jeap.archrepo.importer.messagetype.repository.github;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("resource")
class GitHubMessageTypeRepositoryTest {

    private static final String GIT_URI = "https://github.com/jeap-admin-ch/jeap-message-type-registry.git";

    @Test
    void processBaseUri() {
        GitHubMessageTypeRepository gitHubMessageTypeRepository = new GitHubMessageTypeRepository(GIT_URI);

        assertEquals("https://github.com/jeap-admin-ch/jeap-message-type-registry/blob/-/descriptor",
                gitHubMessageTypeRepository.processBaseUri(GIT_URI));
    }
}
