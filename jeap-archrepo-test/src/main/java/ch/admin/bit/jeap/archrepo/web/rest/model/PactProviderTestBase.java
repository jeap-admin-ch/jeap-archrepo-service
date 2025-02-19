package ch.admin.bit.jeap.archrepo.web.rest.model;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.AllowOverridePactUrl;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.persistence.ApiDocVersion;
import ch.admin.bit.jeap.archrepo.persistence.ArchitectureModelRepository;
import ch.admin.bit.jeap.archrepo.persistence.OpenApiSpecRepository;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import ch.admin.bit.jeap.archrepo.web.ArchRepoApplication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static ch.admin.bit.jeap.archrepo.test.Pacticipants.ARCHREPO;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = ArchRepoApplication.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles({"pact-provider-test"})
@Provider(ARCHREPO)
@PactBroker
@IgnoreNoPactsToVerify
@AllowOverridePactUrl
public class PactProviderTestBase {

    @LocalServerPort
    private int localServerPort;

    @MockitoBean
    ArchitectureModelRepository architectureModelRepository;

    @MockitoBean
    OpenApiSpecRepository openApiSpecRepository;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        // If there are no pacts there will be no context.
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", localServerPort, "/"));
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void testPacts(PactVerificationContext context) {
        // If there are no pacts there will be no context.
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @SneakyThrows
    @State("A model with one system and one service")
    void simpleModel() {
        ArchitectureModel architectureModel = ModelStub.createSimpleModel();
        when(architectureModelRepository.load()).thenReturn(architectureModel);
    }

    @SneakyThrows
    @State("A model with one rest api relation")
    void restApiRelations() {
        ArchitectureModel architectureModel = ModelStub.createSimpleModelWithOneRestApiRelation();
        when(architectureModelRepository.load()).thenReturn(architectureModel);
    }

    @State("A model with one component with an OpenAPI documentation")
    void openApiDocumentationVersions() {
        when(openApiSpecRepository.getApiDocVersions()).thenReturn(
                List.of(new ApiDocVersionImpl("test-system", "test-component", "1.2.3")));
    }
    
    @Value
    private static class ApiDocVersionImpl implements ApiDocVersion {
        String system;
        String component;
        String version;
    }
}