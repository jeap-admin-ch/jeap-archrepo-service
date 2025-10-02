package ch.admin.bit.jeap.archrepo.web.rest.database;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.model.database.*;
import ch.admin.bit.jeap.archrepo.persistence.DatabaseSchemaVersion;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentDatabaseSchemaRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.web.ArchRepoApplication;
import ch.admin.bit.jeap.archrepo.web.service.SystemComponentService;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilder;
import ch.admin.bit.jeap.security.test.jws.JwsBuilderFactory;
import ch.admin.bit.jeap.security.test.resource.configuration.JeapOAuth2IntegrationTestResourceConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.Value;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = ArchRepoApplication.class,
        properties = {  "server.port=8901",
                "jeap.security.oauth2.resourceserver.authorization-server.issuer=" + JwsBuilder.DEFAULT_ISSUER,
                "jeap.security.oauth2.resourceserver.authorization-server.jwk-set-uri=http://localhost:${server.port}/test-app/.well-known/jwks.json"})
@Import(JeapOAuth2IntegrationTestResourceConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatabaseSchemaControllerTest {

    private static final String SYSTEM_NAME = "test-system";
    private static final String COMPONENT_NAME = "test-component";
    private static final String DB_SCHEMA_API_PATH = "/api/dbschemas";

    private static final SemanticApplicationRole DB_SCHEMA_WRITE_ROLE = SemanticApplicationRole.builder()
            .system("application-platform")
            .resource("database-schema")
            .operation("write")
            .build();

    private static final SemanticApplicationRole DB_SCHEMA_OTHER_ROLE = SemanticApplicationRole.builder()
            .system("application-platform")
            .tenant(SYSTEM_NAME)
            .resource("database-schema")
            .operation("other")
            .build();

    private static final String SUBJECT = "69368608-D736-43C8-5F76-55B7BF168299";
    private static final JeapAuthenticationContext CONTEXT = JeapAuthenticationContext.SYS;

    @Autowired
    private JwsBuilderFactory jwsBuilderFactory;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    PlatformTransactionManager platformTransactionManager;

    @MockitoBean
    SystemRepository systemRepository;

    @MockitoBean
    SystemComponentService systemComponentService;

    @MockitoBean
    SystemComponentDatabaseSchemaRepository systemComponentDatabaseSchemaRepository;

    @Captor
    ArgumentCaptor<SystemComponentDatabaseSchema> systemComponentDatabaseSchemaArgumentCaptor;

    @Test
    void testCreateOrUpdateDbSchema_CreateValid() throws Exception {
        final String bearerAuth = createBearerAuthForUserRoles(DB_SCHEMA_WRITE_ROLE);
        final System system = createSystem();
        final SystemComponent systemComponent = system.getSystemComponents().getFirst();
        when(systemComponentService.findOrCreateSystemComponent(COMPONENT_NAME)).thenReturn(systemComponent);
        // mocking no db schema to exist yet
        when(systemComponentDatabaseSchemaRepository.findBySystemComponent(systemComponent)).thenReturn(Optional.empty());
        // capturing the creation of a db schema
        when(systemComponentDatabaseSchemaRepository.saveAndFlush(systemComponentDatabaseSchemaArgumentCaptor.capture())).
                thenAnswer(invocation -> invocation.getArgument(0));
        CreateOrUpdateDbSchemaDto createOrUpdateDbSchemaDto = getCreateOrUpdateDbSchemaDto();
        String content = getJsonString(createOrUpdateDbSchemaDto);

        mockMvc.perform(
                post(DB_SCHEMA_API_PATH).
                        header(HttpHeaders.AUTHORIZATION, bearerAuth).
                        contentType(MediaType.APPLICATION_JSON).
                        content(content).
                        with(csrf())).
                andExpect(status().isCreated());

        verify(systemComponentDatabaseSchemaRepository, times(1)).saveAndFlush(any());
        SystemComponentDatabaseSchema systemComponentDatabaseSchema = systemComponentDatabaseSchemaArgumentCaptor.getValue();
        assertThat(systemComponentDatabaseSchema.getSystemComponent().getName()).isEqualTo(createOrUpdateDbSchemaDto.getSystemComponentName());
        assertThat(systemComponentDatabaseSchema.getSchemaVersion()).isEqualTo(createOrUpdateDbSchemaDto.getSchema().version());
        assertThat(systemComponentDatabaseSchema.getSchema()).isEqualTo(createOrUpdateDbSchemaDto.getSchema().toJson());
    }

    @Test
    void testCreateOrUpdateDbSchema_CreateInvalid() throws Exception {
        final String bearerAuth = createBearerAuthForUserRoles(DB_SCHEMA_WRITE_ROLE);
        String missingTablesContent = """
                            {
                                "systemName": "test-system",
                                    "systemComponentName": "test-component",
                                    "schema": {
                                    "name": "test-schema",
                                    "version": "1.2.3"
                                }
                             }""";

        mockMvc.perform(
                        post(DB_SCHEMA_API_PATH).
                                header(HttpHeaders.AUTHORIZATION, bearerAuth).
                                contentType(MediaType.APPLICATION_JSON).
                                content(missingTablesContent).
                                with(csrf())).
                andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrUpdateDbSchema_Update() throws Exception {
        final String bearerAuth = createBearerAuthForUserRoles(DB_SCHEMA_WRITE_ROLE);
        final System system = createSystem();
        final SystemComponent systemComponent = system.getSystemComponents().getFirst();
        when(systemComponentService.findOrCreateSystemComponent(COMPONENT_NAME)).thenReturn(systemComponent);
        // mocking db schema to exist
        final byte[] dummySerializedSchema = "dummy-schema".getBytes();
        SystemComponentDatabaseSchema existingSchema = SystemComponentDatabaseSchema.builder()
                .systemComponent(systemComponent)
                .schema(dummySerializedSchema)
                .schemaVersion("1.0.0")
                .build();
        when(systemComponentDatabaseSchemaRepository.findBySystemComponent(systemComponent)).
                thenReturn(Optional.of(existingSchema));
        CreateOrUpdateDbSchemaDto createOrUpdateDbSchemaDto = getCreateOrUpdateDbSchemaDto();
        String content = getJsonString(createOrUpdateDbSchemaDto);

        mockMvc.perform(
                        post(DB_SCHEMA_API_PATH).
                                header(HttpHeaders.AUTHORIZATION, bearerAuth).
                                contentType(MediaType.APPLICATION_JSON).
                                content(content).
                                with(csrf())).
                andExpect(status().isOk());

        // verify that the existing schema has been updated
        assertThat(existingSchema.getSchemaVersion()).isEqualTo(createOrUpdateDbSchemaDto.getSchema().version());
        assertThat(existingSchema.getSchema()).isNotEqualTo(dummySerializedSchema);
        verify(systemComponentDatabaseSchemaRepository, never()).saveAndFlush(any());
    }

    @Test
    void testGetDatabaseVersions() throws Exception {
        DatabaseSchemaVersionImpl databaseSchemaVersion1 = new DatabaseSchemaVersionImpl(SYSTEM_NAME, "component1", "1.0.0");
        DatabaseSchemaVersionImpl databaseSchemaVersion2 = new DatabaseSchemaVersionImpl(SYSTEM_NAME, "component2", "2.0.0");
        List<DatabaseSchemaVersion> versions = List.of(
                databaseSchemaVersion1,
                databaseSchemaVersion2
        );
        when(systemComponentDatabaseSchemaRepository.getDatabaseSchemaVersions()).
                thenReturn(versions);

        String json = mockMvc.perform(
                        get(DB_SCHEMA_API_PATH + "/versions").
                                contentType(MediaType.APPLICATION_JSON)).
                andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<DatabaseSchemaVersionImpl> databaseSchemaVersions = objectMapper.readValue(json, new TypeReference<>() {});
        Assertions.assertThat(databaseSchemaVersions).containsExactly(databaseSchemaVersion1, databaseSchemaVersion2);
    }

    @Test
    void testCreateOrUpdateDbSchema_CreateForbiddenRole() throws Exception {
        final String bearerAuth = createBearerAuthForUserRoles(DB_SCHEMA_OTHER_ROLE);
        mockMvc.perform(
                        post(DB_SCHEMA_API_PATH).
                                header(HttpHeaders.AUTHORIZATION, bearerAuth).
                                contentType(MediaType.APPLICATION_JSON).
                                content(getJsonString(getCreateOrUpdateDbSchemaDto())).
                                with(csrf())).
                andExpect(status().isForbidden());
    }

    @Test
    void testCreateOrUpdateDbSchema_CreateUnauthorized() throws Exception {
        mockMvc.perform(
                        post(DB_SCHEMA_API_PATH).
                                contentType(MediaType.APPLICATION_JSON).
                                content(getJsonString(getCreateOrUpdateDbSchemaDto())).
                                with(csrf())).
                andExpect(status().isUnauthorized());
    }

    private CreateOrUpdateDbSchemaDto getCreateOrUpdateDbSchemaDto() {
        Table table1 = Table.builder()
                .name("test_table")
                .columns(List.of(
                        new TableColumn("test_column_1", "text", false),
                        new TableColumn("test_column_2", "text", true)))
                .primaryKey(new TablePrimaryKey("pk_test", List.of("test_column_1")))
                .build();
        Table table2 = Table.builder()
                .name("test-table-2")
                .columns(List.of(
                        new TableColumn("ref", "text", false),
                        new TableColumn("data", "text", true)))
                .foreignKeys(List.of(
                        TableForeignKey.builder()
                                .name("test-fk")
                                .referencedTableName("test_table")
                                .columnNames(List.of("ref"))
                                .referencedColumnNames(List.of("test_column_1"))
                                .build()))
                .build();
        DatabaseSchema databaseSchema = DatabaseSchema.builder()
                .name("test-schema")
                .version("1.2.3")
                .tables(List.of(table1, table2))
                .build();
        return new CreateOrUpdateDbSchemaDto(COMPONENT_NAME, databaseSchema);
    }

    @SneakyThrows
    private String getJsonString(Object o) {
        return objectMapper.writeValueAsString(o);
    }


    private System createSystem() {
        System system = System.builder()
                .name(SYSTEM_NAME)
                .build();
        BackendService backendService = BackendService.builder()
                .name(COMPONENT_NAME)
                .build();
        system.addSystemComponent(backendService);
        return system;
    }

    @Value
    private static class DatabaseSchemaVersionImpl implements DatabaseSchemaVersion {
        String system;
        String component;
        String version;
    }

    private String createBearerAuthForUserRoles(SemanticApplicationRole... userroles) {
        return "Bearer " + jwsBuilderFactory.createValidForFixedLongPeriodBuilder(SUBJECT, CONTEXT).
                withUserRoles(userroles).
                build().serialize();
    }

}
