package ch.admin.bit.jeap.archrepo.web.rest.admin;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationStatus;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.persistence.RestApiRelationRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.persistence.TeamRepository;
import ch.admin.bit.jeap.archrepo.web.config.WebSecurityConfig;
import ch.admin.bit.jeap.archrepo.web.rest.model.ArchRepoWebTestConfiguration;
import ch.admin.bit.jeap.archrepo.web.rest.model.CreateSystemDto;
import ch.admin.bit.jeap.archrepo.web.rest.model.DeleteRestApiDto;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ManagementController.class)
@Import({ArchRepoWebTestConfiguration.class, WebSecurityConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("SameParameterValue")
class ManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    PlatformTransactionManager platformTransactionManager;

    @MockitoBean
    TeamRepository teamRepository;

    @MockitoBean
    SystemRepository systemRepository;

    @MockitoBean
    RestApiRelationRepository restApiRelationRepository;

    @Captor
    ArgumentCaptor<System> systemCaptor;

    private static final String API_USER = "api";
    private static final String API_SECRET = "secret";

    @Captor
    ArgumentCaptor<Team> teamCaptor;


    @Test
    void createSystem_Success_WithAuth() throws Exception {
        String teamName = "Test Team";
        Team team = createTeam(teamName);
        CreateSystemDto createSystemDto = createSystemDto("Test System", "Test Description", 
                "http://confluence.test", List.of("alias1", "alias2"), teamName);

        when(systemRepository.findByNameOrAliasIgnoreCase("Test System")).thenReturn(Optional.empty());
        when(teamRepository.findByName(teamName)).thenReturn(Optional.of(team));
        when(systemRepository.save(any(System.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/management/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSystemDto))
                        .with(httpBasic(API_USER, API_SECRET)))
                .andExpect(status().isCreated());

        verify(systemRepository).save(systemCaptor.capture());
        System savedSystem = systemCaptor.getValue();
        assertEquals("Test System", savedSystem.getName());
        assertEquals("Test Description", savedSystem.getDescription());
        assertEquals("http://confluence.test", savedSystem.getConfluenceLink());
        assertEquals(List.of("alias1", "alias2"), savedSystem.getAliases());
        assertEquals(team, savedSystem.getDefaultOwner());
    }

    @Test
    void createSystem_Unauthorized_WithoutAuth() throws Exception {
        String teamName = "Test Team";
        CreateSystemDto createSystemDto = createMinimalSystemDto("Test System", teamName);

        mockMvc.perform(post("/api/management/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSystemDto)))
                .andExpect(status().isUnauthorized());

        verify(systemRepository, never()).save(any());
    }

    @Test
    void createSystem_SystemAlreadyExists_WithAuth() throws Exception {
        String systemName = "Existing System";
        CreateSystemDto createSystemDto = createMinimalSystemDto(systemName, "Test Team");

        when(systemRepository.findByNameOrAliasIgnoreCase(systemName)).thenReturn(Optional.of(System.builder().name(systemName).build()));

        mockMvc.perform(post("/api/management/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSystemDto))
                        .with(httpBasic(API_USER, API_SECRET)))
                .andExpect(status().isBadRequest());

        verify(systemRepository, never()).save(any());
    }

    @Test
    void createSystem_CreatesNewTeam_WithAuth() throws Exception {
        String teamName = "New Team";
        CreateSystemDto createSystemDto = createMinimalSystemDto("Test System", teamName);

        when(systemRepository.findByNameOrAliasIgnoreCase("Test System")).thenReturn(Optional.empty());
        when(teamRepository.findByName(teamName)).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(systemRepository.save(any(System.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/management/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSystemDto))
                        .with(httpBasic(API_USER, API_SECRET)))
                .andExpect(status().isCreated());

        verify(teamRepository).save(teamCaptor.capture());
        Team savedTeam = teamCaptor.getValue();
        assertEquals(teamName, savedTeam.getName());
        
        verify(systemRepository).save(systemCaptor.capture());
        System savedSystem = systemCaptor.getValue();
        assertEquals("Test System", savedSystem.getName());
        assertEquals(savedTeam, savedSystem.getDefaultOwner());
    }

    @Test
    void createTeam_Success_WithAuth() throws Exception {
        String teamName = "Test Team";
        String contactAddress = "test@example.com";
        String confluenceLink = "http://confluence.test";
        String jiraLink = "http://jira.test";

        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/management/team")
                        .param("name", teamName)
                        .param("contactAddress", contactAddress)
                        .param("confluenceLink", confluenceLink)
                        .param("jiraLink", jiraLink)
                        .with(httpBasic(API_USER, API_SECRET)))
                .andExpect(status().isOk());

        verify(teamRepository).save(teamCaptor.capture());
        Team savedTeam = teamCaptor.getValue();
        assertEquals(teamName, savedTeam.getName());
        assertEquals(contactAddress, savedTeam.getContactAddress());
        assertEquals(confluenceLink, savedTeam.getConfluenceLink());
        assertEquals(jiraLink, savedTeam.getJiraLink());
    }

    @Test
    void createTeam_RequiredParametersOnly_WithAuth() throws Exception {
        String teamName = "Test Team";

        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/management/team")
                        .param("name", teamName)
                        .with(httpBasic(API_USER, API_SECRET)))
                .andExpect(status().isOk());

        verify(teamRepository).save(teamCaptor.capture());
        Team savedTeam = teamCaptor.getValue();
        assertEquals(teamName, savedTeam.getName());
        assertNull(savedTeam.getContactAddress());
        assertNull(savedTeam.getConfluenceLink());
        assertNull(savedTeam.getJiraLink());
    }

    @Test
    void deleteRestApi_Success_WithAuth() throws Exception {
        DeleteRestApiDto deleteDto = createDeleteRestApiDto("provider", "consumer", "GET", "/api/test");
        BackendService mockProvider = createBackendService("provider-component");
        RestApi restApi = createRestApi(mockProvider, "GET", "/api/test");
        RestApiRelation relation = spy(RestApiRelation.builder()
                .restApi(restApi)
                .build());

        when(restApiRelationRepository.findAllByProviderNameAndConsumerNameAndStatus("provider", "consumer", RelationStatus.ACTIVE))
                .thenReturn(List.of(relation));

        mockMvc.perform(delete("/api/management/rest-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteDto))
                        .with(httpBasic(API_USER, API_SECRET)))
                .andExpect(status().isOk())
                .andExpect(content().string("Relation deleted successfully"));

        verify(relation).markDeleted();
    }

    @Test
    void deleteRestApi_NotFound_WithAuth() throws Exception {
        DeleteRestApiDto deleteDto = createDeleteRestApiDto("provider", "consumer", "GET", "/api/test");

        when(restApiRelationRepository.findAllByProviderNameAndConsumerNameAndStatus("provider", "consumer", RelationStatus.ACTIVE))
                .thenReturn(List.of());

        mockMvc.perform(delete("/api/management/rest-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteDto))
                        .with(httpBasic(API_USER, API_SECRET)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No relations found"));

        verify(restApiRelationRepository, never()).save(any());
    }

    @Test
    void deleteRestApi_NoMatchingPath_WithAuth() throws Exception {
        DeleteRestApiDto deleteDto = createDeleteRestApiDto("provider", "consumer", "GET", "/api/test");
        BackendService mockProvider = createBackendService("provider-component");
        RestApi restApi = createRestApi(mockProvider, "GET", "/api/different");
        RestApiRelation relation = spy(RestApiRelation.builder()
                .restApi(restApi)
                .build());

        when(restApiRelationRepository.findAllByProviderNameAndConsumerNameAndStatus("provider", "consumer", RelationStatus.ACTIVE))
                .thenReturn(List.of(relation));

        mockMvc.perform(delete("/api/management/rest-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteDto))
                        .with(httpBasic(API_USER, API_SECRET)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No relations found"));
        
        verify(relation, never()).markDeleted();
    }

    @Test
    void createTeam_Unauthorized_WithoutAuth() throws Exception {
        String teamName = "Test Team";

        mockMvc.perform(post("/api/management/team")
                        .param("name", teamName))
                .andExpect(status().isUnauthorized());

        verify(teamRepository, never()).save(any());
    }

    @Test
    void deleteRestApi_Unauthorized_WithoutAuth() throws Exception {
        DeleteRestApiDto deleteDto = createDeleteRestApiDto("provider", "consumer", "GET", "/api/test");

        mockMvc.perform(delete("/api/management/rest-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deleteDto)))
                .andExpect(status().isUnauthorized());

        verify(restApiRelationRepository, never()).findAllByProviderNameAndConsumerNameAndStatus(any(), any(), any());
    }

    private CreateSystemDto createSystemDto(String name, String description, String confluenceLink,
                                            List<String> aliases, String teamName) {
        return CreateSystemDto.builder()
                .name(name)
                .description(description)
                .confluenceLink(confluenceLink)
                .aliases(aliases)
                .teamName(teamName)
                .build();
    }

    private CreateSystemDto createMinimalSystemDto(String name, String teamName) {
        return CreateSystemDto.builder()
                .name(name)
                .teamName(teamName)
                .build();
    }

    private Team createTeam(String name) {
        return Team.builder().name(name).build();
    }

    private BackendService createBackendService(String name) {
        return BackendService.builder()
                .name(name)
                .build();
    }

    private RestApi createRestApi(BackendService provider, String method, String path) {
        return RestApi.builder()
                .provider(provider)
                .method(method)
                .path(path)
                .build();
    }

    private DeleteRestApiDto createDeleteRestApiDto(String provider, String consumer, String method, String path) {
        return new DeleteRestApiDto(provider, consumer, method, path);
    }

}
