package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.test.Pacticipants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

@Disabled("For development purposes only")
public class PactProviderTest extends PactProviderTestBase {
    @BeforeAll
    static void beforeAll() {
        // Choose the archrepo service pacticipant whose pacts are to be verified.
        Pacticipants.setArchrepoPacticipant("ezv-applicationplatform-archrepo-service");
    }
}
