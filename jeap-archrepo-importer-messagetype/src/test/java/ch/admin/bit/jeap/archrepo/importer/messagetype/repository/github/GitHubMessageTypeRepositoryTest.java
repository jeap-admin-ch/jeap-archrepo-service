package ch.admin.bit.jeap.archrepo.importer.messagetype.repository.github;

import ch.admin.bit.jeap.archrepo.importer.messagetype.MessageTypeImporterProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

@Disabled("Enable and configure it to test the GitHub integration")
class GitHubMessageTypeRepositoryTest {

    @Test
    void testCloneFromGitHub() {
        // Set proxy settings if needed
        System.setProperty("http.proxyHost", "-");
        System.setProperty("https.proxyHost", "-");
        System.setProperty("http.proxyPort", "8080");
        System.setProperty("https.proxyPort", "8080");
        String appId = "--"; // Your GitHub App ID
        String privateKeyPem = """
                -----BEGIN RSA PRIVATE KEY-----
                set your key here
                -----END RSA PRIVATE KEY-----
            """;
        Map<String, String> params = Map.of("GITHUB_APP_ID", appId, "GITHUB_PRIVATE_KEY_PEM", privateKeyPem);
        GitHubMessageTypeRepository gitHubMessageTypeRepository = new GitHubMessageTypeRepository("https://github.com/jeap-admin-ch/jeap.git", params);
        File clonedGitRepo = gitHubMessageTypeRepository.cloneGitRepo();
        Assertions.assertTrue(new File(clonedGitRepo, "README.md").exists());
    }
}
