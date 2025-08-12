package ch.admin.bit.jeap.archrepo.web.rest.reaction;

import ch.admin.bit.jeap.archrepo.persistence.ReactionStatisticsLastModifiedAt;
import ch.admin.bit.jeap.archrepo.persistence.ReactionStatisticsRepository;
import ch.admin.bit.jeap.archrepo.web.config.WebSecurityConfig;
import ch.admin.bit.jeap.archrepo.web.rest.model.ArchRepoWebTestConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ReactionApiController.class, WebSecurityConfig.class})
@Import(ArchRepoWebTestConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReactionApiControllerTest {

    private static final String API_PATH = "/api/reactions/components";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    ReactionStatisticsRepository reactionStatisticsRepository;


    @Test
    void getComponentsWithReactions() throws Exception {
        final ReactionStatisticsLastModifiedAtImpl component1 = new ReactionStatisticsLastModifiedAtImpl("component1", ZonedDateTime.now());
        final ReactionStatisticsLastModifiedAtImpl component2 = new ReactionStatisticsLastModifiedAtImpl("component2", ZonedDateTime.now().minusDays(3));
        when(reactionStatisticsRepository.getMaxLastModifiedAtList()).thenReturn(List.of(component1, component2));

        String json = mockMvc.perform(get(API_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<ReactionStatisticsLastModifiedAtImpl> result = objectMapper.readValue(json, new TypeReference<>() {
        });
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(ReactionStatisticsLastModifiedAtImpl::getComponent).toList()).containsExactly("component1", "component2");
    }

    @Value
    private static class ReactionStatisticsLastModifiedAtImpl implements ReactionStatisticsLastModifiedAt {
        String component;
        ZonedDateTime lastModifiedAt;
    }
}
