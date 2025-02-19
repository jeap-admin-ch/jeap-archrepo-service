package ch.admin.bit.jeap.archrepo.importer.messagetype.contractservice;

import ch.admin.bit.jeap.archrepo.test.Pacticipants;
import org.junit.jupiter.api.BeforeAll;

class MessageContractServicePactTest extends MessageContractServicePactTestBase {
    @BeforeAll
    static void beforeAll() {
        Pacticipants.setArchrepoPacticipant("archrepo-test-service");
        Pacticipants.setMessageContractServicePacticipant("archrepo-messagecontract-test-provider");
    }
}