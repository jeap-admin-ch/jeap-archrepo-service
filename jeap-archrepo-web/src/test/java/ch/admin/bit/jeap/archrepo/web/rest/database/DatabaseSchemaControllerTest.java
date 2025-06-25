package ch.admin.bit.jeap.archrepo.web.rest.database;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.database.*;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.model.database.*;
import ch.admin.bit.jeap.archrepo.persistence.*;
import ch.admin.bit.jeap.archrepo.web.config.WebSecurityConfig;
import ch.admin.bit.jeap.archrepo.web.rest.model.ArchRepoWebTestConfiguration;
import ch.admin.bit.jeap.security.resource.configuration.MvcSecurityConfiguration;
import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.resource.token.TokenConfiguration;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DatabaseSchemaController.class)
@Import({ArchRepoWebTestConfiguration.class, WebSecurityConfig.class,
        MvcSecurityConfiguration.class, ResourceServerProperties.class, TokenConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatabaseSchemaControllerTest {

    private static final String SYSTEM_NAME = "test-system";
    private static final String COMPONENT_NAME = "test-component";
    private static final String DB_SCHEMA_API_PATH = "/api/dbschemas";

    private static final SemanticApplicationRole DB_SCHEMA_WRITE_ROLE_SYSTEM = SemanticApplicationRole.builder()
            .system("application-platform")
            .tenant(SYSTEM_NAME)
            .resource("database-schema")
            .operation("write")
            .build();
    private static final SemanticApplicationRole DB_SCHEMA_WRITE_ROLE_OTHER_SYSTEM = SemanticApplicationRole.builder()
            .system("application-platform")
            .tenant("other-system")
            .resource("database-schema")
            .operation("write")
            .build();
    private static final SemanticApplicationRole DB_SCHEMA_OTHER_ROLE_SYSTEM = SemanticApplicationRole.builder()
            .system("application-platform")
            .tenant(SYSTEM_NAME)
            .resource("database-schema")
            .operation("other")
            .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    PlatformTransactionManager platformTransactionManager;

    @MockitoBean
    SystemRepository systemRepository;

    @MockitoBean
    SystemComponentDatabaseSchemaRepository systemComponentDatabaseSchemaRepository;

    @Captor
    ArgumentCaptor<SystemComponentDatabaseSchema> systemComponentDatabaseSchemaArgumentCaptor;

    @Test
    void testCreateOrUpdateDbSchema_CreateValid() throws Exception {
        final System system = createSystem();
        final SystemComponent systemComponent = system.getSystemComponents().getFirst();
        when(systemRepository.findByNameContainingIgnoreCase(SYSTEM_NAME)).thenReturn(Optional.of(system));
        // mocking no db schema to exist yet
        when(systemComponentDatabaseSchemaRepository.findBySystemComponent(systemComponent)).thenReturn(Optional.empty());
        // capturing the creation of a db schema
        when(systemComponentDatabaseSchemaRepository.saveAndFlush(systemComponentDatabaseSchemaArgumentCaptor.capture())).
                thenAnswer(invocation -> invocation.getArgument(0));
        JeapAuthenticationToken authentication = createAuthenticationForUserRoles(DB_SCHEMA_WRITE_ROLE_SYSTEM);
        CreateOrUpdateDbSchemaDto createOrUpdateDbSchemaDto = getCreateOrUpdateDbSchemaDto();
        String content = getJsonString(createOrUpdateDbSchemaDto);

        mockMvc.perform(
                post(DB_SCHEMA_API_PATH).
                        contentType(MediaType.APPLICATION_JSON).
                        content(content).
                        with(authentication(authentication)).
                        with(csrf())).
                andExpect(status().isCreated());

        verify(systemComponentDatabaseSchemaRepository, times(1)).saveAndFlush(any());
        SystemComponentDatabaseSchema systemComponentDatabaseSchema = systemComponentDatabaseSchemaArgumentCaptor.getValue();
        assertThat(systemComponentDatabaseSchema.getSystem().getName()).isEqualTo(createOrUpdateDbSchemaDto.getSystemName());
        assertThat(systemComponentDatabaseSchema.getSystemComponent().getName()).isEqualTo(createOrUpdateDbSchemaDto.getSystemComponentName());
        assertThat(systemComponentDatabaseSchema.getSchemaVersion()).isEqualTo(createOrUpdateDbSchemaDto.getSchema().version());
        assertThat(systemComponentDatabaseSchema.getSchema()).isEqualTo(createOrUpdateDbSchemaDto.getSchema().toJson());
    }

    @Test
    void testCreateOrUpdateDbSchema_CreateInvalid() throws Exception {
        JeapAuthenticationToken authentication = createAuthenticationForUserRoles(DB_SCHEMA_WRITE_ROLE_SYSTEM);
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
                                contentType(MediaType.APPLICATION_JSON).
                                content(missingTablesContent).
                                with(authentication(authentication)).
                                with(csrf())).
                andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrUpdateDbSchema_Update() throws Exception {
        final System system = createSystem();
        final SystemComponent systemComponent = system.getSystemComponents().getFirst();
        when(systemRepository.findByNameContainingIgnoreCase(SYSTEM_NAME)).thenReturn(Optional.of(system));
        // mocking db schema to exist
        final byte[] dummySerializedSchema = "dummy-schema".getBytes();
        SystemComponentDatabaseSchema existingSchema = SystemComponentDatabaseSchema.builder()
                .systemComponent(systemComponent)
                .schema(dummySerializedSchema)
                .schemaVersion("1.0.0")
                .build();
        when(systemComponentDatabaseSchemaRepository.findBySystemComponent(systemComponent)).
                thenReturn(Optional.of(existingSchema));
        JeapAuthenticationToken authentication = createAuthenticationForUserRoles(DB_SCHEMA_WRITE_ROLE_SYSTEM);
        CreateOrUpdateDbSchemaDto createOrUpdateDbSchemaDto = getCreateOrUpdateDbSchemaDto();
        String content = getJsonString(createOrUpdateDbSchemaDto);

        mockMvc.perform(
                        post(DB_SCHEMA_API_PATH).
                                contentType(MediaType.APPLICATION_JSON).
                                content(content).
                                with(authentication(authentication)).
                                with(csrf())).
                andExpect(status().isOk());

        // verify that the existing schema has been updated
        assertThat(existingSchema.getSchemaVersion()).isEqualTo(createOrUpdateDbSchemaDto.getSchema().version());
        assertThat(existingSchema.getSchema()).isNotEqualTo(dummySerializedSchema);
        verify(systemComponentDatabaseSchemaRepository, never()).saveAndFlush(any());
    }

    @Test
    void testCreateOrUpdateDbSchema_CreateForbiddenSystem() throws Exception {
        JeapAuthenticationToken authentication = createAuthenticationForUserRoles(DB_SCHEMA_WRITE_ROLE_OTHER_SYSTEM);
        mockMvc.perform(
                        post(DB_SCHEMA_API_PATH).
                                contentType(MediaType.APPLICATION_JSON).
                                content(getJsonString(getCreateOrUpdateDbSchemaDto())).
                                with(authentication(authentication)).
                                with(csrf())).
                andExpect(status().isForbidden());
    }

    @Test
    void testCreateOrUpdateDbSchema_CreateForbiddenRole() throws Exception {
        JeapAuthenticationToken authentication = createAuthenticationForUserRoles(DB_SCHEMA_OTHER_ROLE_SYSTEM);
        mockMvc.perform(
                        post(DB_SCHEMA_API_PATH).
                                contentType(MediaType.APPLICATION_JSON).
                                content(getJsonString(getCreateOrUpdateDbSchemaDto())).
                                with(authentication(authentication)).
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
        return new CreateOrUpdateDbSchemaDto(SYSTEM_NAME, COMPONENT_NAME, databaseSchema);
    }

    @SneakyThrows
    private String getJsonString(Object o) {
        return objectMapper.writeValueAsString(o);
    }

    private JeapAuthenticationToken createAuthenticationForUserRoles(SemanticApplicationRole... userroles)  {
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(userroles).build();
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

}
