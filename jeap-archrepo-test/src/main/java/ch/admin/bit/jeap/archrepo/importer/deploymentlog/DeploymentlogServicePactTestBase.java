package ch.admin.bit.jeap.archrepo.importer.deploymentlog;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import ch.admin.bit.jeap.archrepo.importer.deploymentlog.client.ComponentVersionSummaryDto;
import ch.admin.bit.jeap.archrepo.importer.deploymentlog.client.DeploymentlogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Base64;
import java.util.List;

import static ch.admin.bit.jeap.archrepo.test.Pacticipants.ARCHREPO;
import static ch.admin.bit.jeap.archrepo.test.Pacticipants.DEPLOYMENTLOG;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(port = "8888", pactVersion = PactSpecVersion.V3)
public class DeploymentlogServicePactTestBase {

    private static final String API_PATH = "/api/environment/ref/components";

    @SuppressWarnings("DataFlowIssue")
    @Pact(provider = DEPLOYMENTLOG, consumer = ARCHREPO)
    private RequestResponsePact getDeployedComponents(PactDslWithProvider builder) {
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());
        return builder.given("Currently deployed components for the stage 'ref' are available")
                .uponReceiving("A GET request to " + API_PATH)
                .path(API_PATH)
                .method("GET")
                .matchHeader("Authorization", "Basic " + basicAuth, "Basic " + basicAuth)
                .willRespondWith()
                .status(200)
                .matchHeader("Content-Type", "application/json", "application/json; encoding=utf-8")
                .body(PactDslJsonArray.arrayEachLike()
                        .stringValue("componentName", "some-system-component")
                        .stringValue("version", "1.0.0")
                        .closeObject())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getDeployedComponents")
    void testGetContracts() {
        // given
        DeploymentlogProperties props = new DeploymentlogProperties();
        props.setUrl("http://localhost:8888");
        props.setUsername("user");
        props.setPassword("secret");
        DeploymentlogService deploymentlogService = new DeploymentlogImporterConfiguration()
                .deploymentlogService(props, "test");

        // when
        List<ComponentVersionSummaryDto> result = deploymentlogService.getDeployedComponents("ref");

        // then
        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().componentName())
                .isEqualTo("some-system-component");
    }
}