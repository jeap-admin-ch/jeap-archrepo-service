package ch.admin.bit.jeap.archrepo.web.rest.database;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.database.*;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.model.database.DatabaseSchema;
import ch.admin.bit.jeap.archrepo.model.database.Table;
import ch.admin.bit.jeap.archrepo.model.database.TableColumn;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentDatabaseSchemaRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExternalDatabaseSchemaController.class)
@Import({ArchRepoWebTestConfiguration.class, WebSecurityConfig.class,
        MvcSecurityConfiguration.class, ResourceServerProperties.class, TokenConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExternalDatabaseSchemaControllerTest {

    private static final String COMPONENT_NAME = "test-component";
    private static final String EXTERNAL_DB_SCHEMA_API_PATH = "/external-api/dbschemas";

    private static final SemanticApplicationRole EXTERNAL_DB_SCHEMA_READ_ROLE = SemanticApplicationRole.builder()
            .system("application-platform")
            .resource("external-database-schema")
            .operation("read")
            .build();
    private static final SemanticApplicationRole EXTERNAL_DB_SCHEMA_OTHER_ROLE = SemanticApplicationRole.builder()
            .system("application-platform")
            .resource("external-database-schema")
            .operation("other")
            .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    SystemComponentRepository systemComponentRepository;

    @MockitoBean
    SystemComponentDatabaseSchemaRepository systemComponentDatabaseSchemaRepository;

    @Test
    void testGetDatabaseSchema_Found() throws Exception {
        final SystemComponent systemComponent = createSystemComponent();
        when(systemComponentRepository.findByNameIgnoreCase(COMPONENT_NAME)).thenReturn(Optional.of(systemComponent));
        // mocking db schema to exist
        DatabaseSchema databaseSchema = getDatabaseSchema();
        SystemComponentDatabaseSchema existingSystemComponentDatabaseSchema = SystemComponentDatabaseSchema.builder()
                .systemComponent(systemComponent)
                .schema(databaseSchema.toJson())
                .schemaVersion(databaseSchema.version())
                .build();
        when(systemComponentDatabaseSchemaRepository.findBySystemComponent(systemComponent)).
                thenReturn(Optional.of(existingSystemComponentDatabaseSchema));
        JeapAuthenticationToken authentication = createAuthenticationForUserRoles(EXTERNAL_DB_SCHEMA_READ_ROLE);

        String result = mockMvc.perform(
                get(EXTERNAL_DB_SCHEMA_API_PATH).
                        accept(MediaType.APPLICATION_JSON).
                        queryParam("systemComponentName", COMPONENT_NAME).
                        with(authentication(authentication))).
                andExpect(status().isOk()).
                andReturn().
                getResponse().
                getContentAsString();

        DatabaseSchemaDto resultDto = getDatabaseSchemaDtoFromJsonString(result);
        assertThat(resultDto.getSystemComponentName()).isEqualTo(COMPONENT_NAME);
        assertThat(resultDto.getSchema()).isEqualTo(databaseSchema);
    }

    @Test
    void testGetDatabaseSchema_NotFoundSystemComponent() throws Exception {
        when(systemComponentRepository.findByNameIgnoreCase(COMPONENT_NAME)).thenReturn(Optional.empty());
        JeapAuthenticationToken authentication = createAuthenticationForUserRoles(EXTERNAL_DB_SCHEMA_READ_ROLE);

        mockMvc.perform(
                        get(EXTERNAL_DB_SCHEMA_API_PATH).
                                accept(MediaType.APPLICATION_JSON).
                                queryParam("systemComponentName", "other-component").
                                with(authentication(authentication))).
                andExpect(status().isNotFound());
    }

    @Test
    void testGetDatabaseSchema_NotFoundSchema() throws Exception {
        final SystemComponent systemComponent = createSystemComponent();
        when(systemComponentRepository.findByNameIgnoreCase(COMPONENT_NAME)).thenReturn(Optional.of(systemComponent));
        when(systemComponentDatabaseSchemaRepository.findBySystemComponent(systemComponent)).thenReturn(Optional.empty());
        JeapAuthenticationToken authentication = createAuthenticationForUserRoles(EXTERNAL_DB_SCHEMA_READ_ROLE);

        mockMvc.perform(
                        get(EXTERNAL_DB_SCHEMA_API_PATH).
                                accept(MediaType.APPLICATION_JSON).
                                queryParam("systemComponentName", COMPONENT_NAME).
                                with(authentication(authentication))).
                andExpect(status().isNotFound());
    }

    @Test
    void testGetDatabaseSchema_OtherRole() throws Exception {
        JeapAuthenticationToken authentication = createAuthenticationForUserRoles(EXTERNAL_DB_SCHEMA_OTHER_ROLE);

        mockMvc.perform(
                        get(EXTERNAL_DB_SCHEMA_API_PATH).
                                accept(MediaType.APPLICATION_JSON).
                                queryParam("systemComponentName", COMPONENT_NAME).
                                with(authentication(authentication))).
                andExpect(status().isForbidden());
    }

    @Test
    void testGetDatabaseSchema_Unauthorized() throws Exception {
        mockMvc.perform(
                        get(EXTERNAL_DB_SCHEMA_API_PATH).
                                accept(MediaType.APPLICATION_JSON).
                                queryParam("systemComponentName", COMPONENT_NAME)).
                andExpect(status().isUnauthorized());
    }

    private DatabaseSchema getDatabaseSchema() {
        Table table = Table.builder()
                .name("test_table")
                .columns(List.of(new TableColumn("test_column", "text", false)))
                .build();
        return DatabaseSchema.builder()
                .name("test-schema")
                .version("1.2.3")
                .tables(List.of(table))
                .build();
    }

    @SneakyThrows
    private DatabaseSchemaDto getDatabaseSchemaDtoFromJsonString(String json) {
        return objectMapper.readValue(json, DatabaseSchemaDto.class);
    }

    private JeapAuthenticationToken createAuthenticationForUserRoles(SemanticApplicationRole... userroles)  {
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(userroles).build();
    }

    private SystemComponent createSystemComponent() {
        System system = System.builder()
                .name("test-system")
                .build();
        BackendService backendService = BackendService.builder()
                .name(COMPONENT_NAME)
                .build();
        system.addSystemComponent(backendService);
        return backendService;
    }

}
