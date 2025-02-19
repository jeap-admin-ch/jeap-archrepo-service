package ch.admin.bit.jeap.archrepo.importer.deploymentlog;

import ch.admin.bit.jeap.archrepo.test.Pacticipants;
import org.junit.jupiter.api.BeforeAll;

class DeploymentlogServicePactTest extends DeploymentlogServicePactTestBase {
    @BeforeAll
    static void beforeAll() {
        Pacticipants.setArchrepoPacticipant("archrepo-test-service");
        Pacticipants.setDeploymentlogPacticipant("archrepo-deploymentlog-test-provider");
    }
}