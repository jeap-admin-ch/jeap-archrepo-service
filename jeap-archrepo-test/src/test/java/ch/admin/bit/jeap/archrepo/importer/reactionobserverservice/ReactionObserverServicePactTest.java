package ch.admin.bit.jeap.archrepo.importer.reactionobserverservice;

import ch.admin.bit.jeap.archrepo.importer.reaction.ReactionObserverServicePactTestBase;
import ch.admin.bit.jeap.archrepo.test.Pacticipants;
import org.junit.jupiter.api.BeforeAll;

public class ReactionObserverServicePactTest extends ReactionObserverServicePactTestBase {
    @BeforeAll
    static void beforeAll() {
        Pacticipants.setArchrepoPacticipant("archrepo-test-service");
        Pacticipants.setReactionObserverServicePacticipant("archrepo-reactionobserverservice-test-provider");
    }

}
