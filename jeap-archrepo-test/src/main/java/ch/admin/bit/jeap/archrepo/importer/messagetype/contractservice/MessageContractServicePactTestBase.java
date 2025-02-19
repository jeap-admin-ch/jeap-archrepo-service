package ch.admin.bit.jeap.archrepo.importer.messagetype.contractservice;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import ch.admin.bit.jeap.archrepo.importer.messagetype.MessageTypeImporterProperties;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.util.List;

import static ch.admin.bit.jeap.archrepo.test.Pacticipants.ARCHREPO;
import static ch.admin.bit.jeap.archrepo.test.Pacticipants.MCS;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("WrongPropertyKeyValueDelimiter")
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(port = "8888", pactVersion = PactSpecVersion.V3)
public class MessageContractServicePactTestBase {

    private static final String API_PATH = "/api/contracts";

    private static String dummyGitRepoUrl;

    @BeforeAll
    static void beforeAll() throws Exception {
        File repoDir = new File("target/dummy-git-repo");
        FileUtils.copyDirectory(new File("src/test/resources/dummy-git-repo"), repoDir);
        Git newRepo = Git.init()
                .setDirectory(repoDir)
                .call();
        newRepo.add()
                .addFilepattern(".")
                .call();
        newRepo.commit()
                .setMessage("Initial revision")
                .call();

        newRepo.close();
        dummyGitRepoUrl = "file://" + repoDir.getAbsolutePath();
    }


    @SuppressWarnings("DataFlowIssue")
    @Pact(provider = MCS, consumer = ARCHREPO)
    private RequestResponsePact requestContractsNonEmpty(PactDslWithProvider builder) {
        // @formatter:off
        return builder.given("Contracts for the stage 'ref' are available.").
                uponReceiving("A GET request to " + API_PATH).
                path(API_PATH).
                query("env=ref").
                method("GET").
                willRespondWith().
                status(200).
                matchHeader("Content-Type", "application/json", "application/json; encoding=utf-8").
                body(PactDslJsonArray.arrayEachLike().
                        stringValue("appName", "application-1").
                        stringValue("messageType", "InputDocArchivedEvent").
                        stringValue("messageTypeVersion", "2.0.0").
                        stringValue("topic", "given-topic").
                        stringValue("role", "CONSUMER").
                        closeObject()).
                toPact();
        // @formatter:on
    }

    @Test
    @PactTestFor(pactMethod = "requestContractsNonEmpty")
    void testGetContracts() {
        // given
        MessageTypeImporterProperties properties = new MessageTypeImporterProperties(
                List.of(dummyGitRepoUrl),
                "http://localhost:8888/api/contracts?env=ref");
        ContractServiceClient contractServiceClient = new ContractServiceClient(properties, RestClient.builder());

        // when
        List<MessageContractDto> result = contractServiceClient.getMessageContracts();

        // then
        assertThat(result).isNotEmpty();
        MessageContractDto contract = result.getFirst();
        assertThat(contract.appName()).isEqualTo("application-1");
        assertThat(contract.messageType()).isEqualTo("InputDocArchivedEvent");
        assertThat(contract.messageTypeVersion()).isEqualTo("2.0.0");
        assertThat(contract.topic()).isEqualTo("given-topic");
        assertThat(contract.role()).isIn(List.of(MessageContractRole.values()));
    }

    @SuppressWarnings("DataFlowIssue")
    @Pact(provider = MCS, consumer = ARCHREPO)
    private RequestResponsePact requestContractsEmptyList(PactDslWithProvider builder) {
        // @formatter:off
        return builder.given("Contracts for the stage 'ref' are empty.").
                uponReceiving("A GET request to " + API_PATH).
                path(API_PATH).
                query("env=ref").
                method("GET").
                willRespondWith().
                status(200).
                matchHeader("Content-Type", "application/json", "application/json; encoding=utf-8").
                body(PactDslJsonArray.arrayEachLike(0).
                        closeObject()).
                toPact();
        // @formatter:on
    }

    @Test
    @PactTestFor(pactMethod = "requestContractsEmptyList")
    void testGetEmptyList() {
        // given
        MessageTypeImporterProperties properties = new MessageTypeImporterProperties(
                List.of(dummyGitRepoUrl),
                "http://localhost:8888/api/contracts?env=ref");
        ContractServiceClient contractServiceClient = new ContractServiceClient(properties, RestClient.builder());

        // when
        List<MessageContractDto> result = contractServiceClient.getMessageContracts();

        // then
        assertThat(result)
                .isNotNull()
                .isEmpty();
    }
}