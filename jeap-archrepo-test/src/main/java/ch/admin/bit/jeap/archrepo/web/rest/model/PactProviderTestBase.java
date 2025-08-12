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
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.model.database.*;
import ch.admin.bit.jeap.archrepo.persistence.*;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import ch.admin.bit.jeap.archrepo.metamodel.System;

import ch.admin.bit.jeap.archrepo.web.ArchRepoApplication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static ch.admin.bit.jeap.archrepo.test.Pacticipants.ARCHREPO;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SuppressWarnings("unused")
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

    @MockitoBean
    ReactionStatisticsRepository reactionStatisticsRepository;

    @MockitoBean
    SystemRepository systemRepository;

    @MockitoBean
    SystemComponentDatabaseSchemaRepository systemComponentDatabaseSchemaRepository;

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

    @State("A model with one component with observed reactions")
    void reactionStatistics() {
        when(reactionStatisticsRepository.getMaxLastModifiedAtList()).thenReturn(
                List.of(new ReactionStatisticsLastModifiedAtImpl("test-component", ZonedDateTime.of(2025, 8, 1, 14, 0, 0 ,0, ZoneId.of("UTC")))));
    }

    @State("A database schema exists for the component 'test-component' in the system 'test-system'")
    @SneakyThrows
    void databaseSchemaExists() {
        SystemComponent systemComponent = mockSystemAndComponent("test-system", "test-component");
        DatabaseSchema databaseSchema = getFullDatabaseSchema();
        SystemComponentDatabaseSchema systemComponentDatabaseSchema = SystemComponentDatabaseSchema.builder().
                systemComponent(systemComponent)
                .schema(databaseSchema.toJson())
                .schemaVersion(databaseSchema.version())
                .build();
        when(systemComponentDatabaseSchemaRepository.findBySystemComponent(systemComponent)).
                thenReturn(Optional.of(systemComponentDatabaseSchema));
    }

    @State("No database schema exists for the component 'test-component' in the system 'test-system'")
    void noDatabaseSchemaExists() {
        SystemComponent systemComponent = mockSystemAndComponent("test-system", "test-component");
        when(systemComponentDatabaseSchemaRepository.findBySystemComponent(systemComponent)).
                thenReturn(Optional.empty());
    }

    @SuppressWarnings("SameParameterValue")
    private SystemComponent mockSystemAndComponent(String systemName, String systemComponentName) {
        System system = mock(System.class);
        when(system.getName()).thenReturn(systemName);
        SystemComponent systemComponent = mock(SystemComponent.class);
        when(systemComponent.getName()).thenReturn(systemComponentName);
        when(systemComponent.getParent()).thenReturn(system);
        when(system.findSystemComponent(systemComponentName)).thenReturn(Optional.of(systemComponent));
        when(systemRepository.findByNameContainingIgnoreCase(systemName)).thenReturn(Optional.of(system));
        return systemComponent;
    }

    private DatabaseSchema getFullDatabaseSchema() {
        Table tableA = Table.builder()
                .name("table_a")
                .columns(List.of(new TableColumn("column_a", "text", false)))
                .primaryKey(new TablePrimaryKey("pk_a", List.of("column_a")))
                .build();
        Table tableB = Table.builder()
                .name("table_b")
                .columns(List.of(new TableColumn("column_b", "text", false)))
                .columns(List.of(new TableColumn("column_c", "text", true)))
                .foreignKeys(List.of(TableForeignKey.builder().
                                name("fk_a_b")
                                .columnNames(List.of("column_b"))
                                .referencedColumnNames(List.of("column_a"))
                                .build()))
                .build();
        return DatabaseSchema.builder()
                .name("test-schema")
                .version("1.2.3")
                .tables(List.of(tableA, tableB))
                .build();
    }
    
    @Value
    private static class ApiDocVersionImpl implements ApiDocVersion {
        String system;
        String component;
        String version;
    }

    @Value
    private static class ReactionStatisticsLastModifiedAtImpl implements ReactionStatisticsLastModifiedAt {
        String component;
        ZonedDateTime lastModifiedAt;
    }
}
