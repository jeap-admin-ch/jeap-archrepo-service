package ch.admin.bit.jeap.archrepo.web.rest.admin;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationStatus;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.persistence.RestApiRelationRepository;
import ch.admin.bit.jeap.archrepo.web.config.WebSecurityConfig;
import ch.admin.bit.jeap.archrepo.web.rest.model.ArchRepoWebTestConfiguration;
import ch.admin.bit.jeap.archrepo.web.rest.model.DeleteRestApiDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ManagementController.class)
@Import({ArchRepoWebTestConfiguration.class, WebSecurityConfig.class})
@ActiveProfiles("test")
class ManagementControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    PlatformTransactionManager platformTransactionManager;
    @MockitoBean
    RestApiRelationRepository restApiRelationRepository;

    @Test
    void deleteRestApi() throws Exception {
        RestApiRelation relation = relation("/api/test");
        when(restApiRelationRepository.findAllByProviderNameAndConsumerNameAndStatus("provider", "consumer", RelationStatus.ACTIVE))
                .thenReturn(List.of(relation));

        mockMvc.perform(delete("/api/management/rest-api")
                        .contentType(MediaType.APPLICATION_JSON).content(request())
                        .with(httpBasic("archrepo-admin", "secret")))
                .andExpect(status().isOk())
                .andExpect(content().string("Relation deleted successfully"));
        verify(relation).markDeleted();
    }

    @Test
    void deleteRestApiNotFound() throws Exception {
        mockMvc.perform(delete("/api/management/rest-api")
                        .contentType(MediaType.APPLICATION_JSON).content(request())
                        .with(httpBasic("archrepo-admin", "secret")))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No relations found"));
        verify(restApiRelationRepository, never()).save(any());
    }

    @Test
    void deleteRestApiNoMatchingPath() throws Exception {
        RestApiRelation relation = relation("/api/different");
        when(restApiRelationRepository.findAllByProviderNameAndConsumerNameAndStatus("provider", "consumer", RelationStatus.ACTIVE))
                .thenReturn(List.of(relation));
        mockMvc.perform(delete("/api/management/rest-api")
                        .contentType(MediaType.APPLICATION_JSON).content(request())
                        .with(httpBasic("archrepo-admin", "secret")))
                .andExpect(status().isNotFound());
        verify(relation, never()).markDeleted();
    }

    @Test
    void deleteRestApiUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/management/rest-api")
                        .contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(restApiRelationRepository);
    }

    @Test
    void systemAndTeamCreationEndpointsAreRemoved() throws Exception {
        for (String endpoint : List.of("system", "team")) {
            mockMvc.perform(post("/api/management/" + endpoint)
                            .contentType(MediaType.APPLICATION_JSON).content("{}")
                            .with(httpBasic("archrepo-admin", "secret")))
                    .andExpect(status().isNotFound());
        }
        verifyNoInteractions(restApiRelationRepository);
    }

    private String request() {
        return objectMapper.writeValueAsString(new DeleteRestApiDto("provider", "consumer", "GET", "/api/test"));
    }

    private RestApiRelation relation(String path) {
        return spy(RestApiRelation.builder().restApi(RestApi.builder()
                .provider(BackendService.builder().name("provider-component").build())
                .method("GET").path(path).build()).build());
    }
}
