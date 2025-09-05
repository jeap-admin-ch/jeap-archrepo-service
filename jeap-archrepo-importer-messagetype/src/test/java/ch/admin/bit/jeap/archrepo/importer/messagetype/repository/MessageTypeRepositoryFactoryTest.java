package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import ch.admin.bit.jeap.archrepo.importer.messagetype.MessageTypeImporterProperties;
import ch.admin.bit.jeap.archrepo.importer.messagetype.RepositoryProperties;
import ch.admin.bit.jeap.archrepo.importer.messagetype.repository.github.GitHubMessageTypeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageTypeRepositoryFactoryTest {

    @Test
    void testWithBitbucketRepository() {
        MessageTypeImporterProperties properties = new MessageTypeImporterProperties();
        properties.setRepositories(
                List.of(new RepositoryProperties("https://bitbucket.example.com/scm/proj/repo.git", RepositoryProperties.RepositoryType.BITBUCKET))
        );
        MessageTypeRepositoryFactory messageTypeRepositoryFactory = new MessageTypeRepositoryFactory(properties);
        List<MessageTypeRepository> repositories = messageTypeRepositoryFactory.getRepositories();
        assertNotNull(repositories);
        assertEquals(1, repositories.size());
        assertInstanceOf(BitbucketMessageTypeRepository.class, repositories.getFirst());
    }

    @Test
    void testWithBitbucketLegacyProperties() {
        MessageTypeImporterProperties properties = new MessageTypeImporterProperties();
        properties.setGitUris(List.of("https://bitbucket.example.com/scm/proj/repo.git"));
        MessageTypeRepositoryFactory messageTypeRepositoryFactory = new MessageTypeRepositoryFactory(properties);
        List<MessageTypeRepository> repositories = messageTypeRepositoryFactory.getRepositories();
        assertNotNull(repositories);
        assertEquals(1, repositories.size());
        assertInstanceOf(BitbucketMessageTypeRepository.class, repositories.getFirst());
    }

    @Test
    void testWithGitHubRepository() {
        MessageTypeImporterProperties properties = new MessageTypeImporterProperties();
        // Sample key from https://phpseclib.com/docs/rsa-keys
        Map<String, String> params = Map.of("GITHUB_APP_ID", "12345", "GITHUB_PRIVATE_KEY_PEM", """
                -----BEGIN RSA PRIVATE KEY-----
                MIIBOgIBAAJBAKj34GkxFhD90vcNLYLInFEX6Ppy1tPf9Cnzj4p4WGeKLs1Pt8Qu
                KUpRKfFLfRYC9AIKjbJTWit+CqvjWYzvQwECAwEAAQJAIJLixBy2qpFoS4DSmoEm
                o3qGy0t6z09AIJtH+5OeRV1be+N4cDYJKffGzDa88vQENZiRm0GRq6a+HPGQMd2k
                TQIhAKMSvzIBnni7ot/OSie2TmJLY4SwTQAevXysE2RbFDYdAiEBCUEaRQnMnbp7
                9mxDXDf6AU0cN/RPBjb9qSHDcWZHGzUCIG2Es59z8ugGrDY+pxLQnwfotadxd+Uy
                v/Ow5T0q5gIJAiEAyS4RaI9YG8EWx/2w0T67ZUVAw8eOMB6BIUg0Xcu+3okCIBOs
                /5OiPgoTdSy7bcF9IGpSE8ZgGKzgYQVZeN97YE00
                -----END RSA PRIVATE KEY-----
                """);
        properties.setRepositories(
                List.of(new RepositoryProperties("https://github.com/jeap-admin-ch/jeap.git", RepositoryProperties.RepositoryType.GITHUB, params))
        );
        MessageTypeRepositoryFactory messageTypeRepositoryFactory = new MessageTypeRepositoryFactory(properties);
        List<MessageTypeRepository> repositories = messageTypeRepositoryFactory.getRepositories();
        assertNotNull(repositories);
        assertEquals(1, repositories.size());
        assertInstanceOf(GitHubMessageTypeRepository.class, repositories.getFirst());
    }

    @Test
    void testWithGitHubRepositoryFailForNoGitHubAppId() {
        MessageTypeImporterProperties properties = new MessageTypeImporterProperties();
        Map<String, String> params = Map.of();
        RepositoryProperties props = new RepositoryProperties("https://github.com/jeap-admin-ch/jeap.git", RepositoryProperties.RepositoryType.GITHUB, params);
        properties.setRepositories(
                List.of(props)
        );
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new MessageTypeRepositoryFactory(properties));
        assertEquals("Missing required parameter 'GITHUB_APP_ID' for GitHub repository", exception.getMessage());
    }

    @Test
    void testWithGitHubRepositoryFailForNoGitHubAppPrivateKey() {
        MessageTypeImporterProperties properties = new MessageTypeImporterProperties();
        Map<String, String> params = Map.of("GITHUB_APP_ID", "12345");
        RepositoryProperties props = new RepositoryProperties("https://github.com/jeap-admin-ch/jeap.git", RepositoryProperties.RepositoryType.GITHUB, params);
        properties.setRepositories(
                List.of(props)
        );
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new MessageTypeRepositoryFactory(properties));
        assertEquals("Missing required parameter 'GITHUB_PRIVATE_KEY_PEM' for GitHub repository", exception.getMessage());
    }
}
