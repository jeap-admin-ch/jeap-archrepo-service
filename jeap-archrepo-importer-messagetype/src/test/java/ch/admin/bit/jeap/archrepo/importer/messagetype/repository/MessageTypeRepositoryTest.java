package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import ch.admin.bit.jeap.archrepo.importer.messagetype.CreateLocalGitRepoBaseTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
class MessageTypeRepositoryTest extends CreateLocalGitRepoBaseTest {

    @Test
    void cannotConnectToRepo() {
        assertThrows(RuntimeException.class, () -> new MessageTypeRepository("https://someurl.com"));
    }

    @Test
    void connectToValidRepo() {
        MessageTypeRepository messageTypeRepository = new MessageTypeRepository(repoUrl);
        assertDoesNotThrow(messageTypeRepository::getAllEventDescriptors);
    }

    @Test
    void testGetAllEventDescriptors() {
        MessageTypeRepository messageTypeRepository = new MessageTypeRepository(repoUrl);

        List<EventDescriptor> results = messageTypeRepository.getAllEventDescriptors();

        assertFalse(results.isEmpty(), "No events found");
        List<EventDescriptor> transparenzaEvents = results.stream()
                .filter(e -> e.getDefiningSystem().equals("TESTSYSTEM"))
                .toList();
        assertFalse(transparenzaEvents.isEmpty(), "No TESTSYSTEM events found");
        List<EventDescriptor> testArchiveDataCreatedEvent = results.stream().filter(e -> e.getMessageTypeName().equals("TestEvent")).toList();
        assertEquals(1, testArchiveDataCreatedEvent.size(), "Not exactly one TestEvent");
    }

    @Test
    void testGetAllCommandDescriptors() {
        MessageTypeRepository messageTypeRepository = new MessageTypeRepository(repoUrl);

        List<CommandDescriptor> results = messageTypeRepository.getAllCommandDescriptors();

        assertFalse(results.isEmpty(), "No commands found");
        List<CommandDescriptor> jmeCommands = results.stream()
                .filter(c -> c.getDefiningSystem().equals("TESTSYSTEM"))
                .toList();
        assertFalse(jmeCommands.isEmpty(), "No TESTSYSTEM commands found");
        List<CommandDescriptor> registerArchivedArtifactCommand = results.stream()
                .filter(c -> c.getMessageTypeName().equals("TestCommand"))
                .toList();
        assertEquals(1, registerArchivedArtifactCommand.size(), "Not exactly one TestCommand");
    }

    @Test
    void processBaseUri() {
        String httpLinkUri = MessageTypeRepository.processBaseUri(
                "https://some-bitbucket-repo.com/scm/projectname/message-type-registry.git");

        assertEquals("https://some-bitbucket-repo.com/projects/PROJECTNAME/repos/message-type-registry/", httpLinkUri);
    }
}
