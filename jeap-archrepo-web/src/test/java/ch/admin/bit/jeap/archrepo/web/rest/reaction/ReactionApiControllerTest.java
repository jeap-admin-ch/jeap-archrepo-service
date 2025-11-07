package ch.admin.bit.jeap.archrepo.web.rest.reaction;


import ch.admin.bit.jeap.archrepo.persistence.ComponentGraphRepository;
import ch.admin.bit.jeap.archrepo.persistence.ReactionLastModifiedAt;
import ch.admin.bit.jeap.archrepo.web.config.WebSecurityConfig;
import ch.admin.bit.jeap.archrepo.web.rest.model.ArchRepoWebTestConfiguration;
import ch.admin.bit.jeap.archrepo.web.rest.model.ReactionLastModifiedAtDto;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ReactionApiController.class, WebSecurityConfig.class})
@Import(ArchRepoWebTestConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReactionApiControllerTest {
    private
    static final String API_PATH = "/api/reactions/components";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    ComponentGraphRepository componentGraphRepository;

    @Test
    void getComponentsWithReactions_shouldReturnExpectedDtos() throws Exception {
        // Arrange
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime earlier = now.minusDays(3);

        ReactionLastModifiedAt projection1 = new TestProjection("component1", now.minusDays(5), now);
        ReactionLastModifiedAt projection2 = new TestProjection("component2", earlier, earlier.minusHours(1));

        when(componentGraphRepository.getMaxCreatedAndModifiedAtList())
                .thenReturn(List.of(projection1, projection2));

        // Act
        String json = mockMvc.perform(get(API_PATH)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<ReactionLastModifiedAtDto> result = objectMapper.readValue(json, new TypeReference<>() {
        });

        // Assert
        assertThat(result).hasSize(2);

        ReactionLastModifiedAtDto dto1 = result.stream()
                .filter(r -> r.component().equals("component1"))
                .findFirst()
                .orElseThrow();

        ReactionLastModifiedAtDto dto2 = result.stream()
                .filter(r -> r.component().equals("component2"))
                .findFirst()
                .orElseThrow();

        assertThat(dto1.lastModifiedAt().toInstant()).isCloseTo(now.toInstant(), within(1, ChronoUnit.MILLIS));
        assertThat(dto2.lastModifiedAt().toInstant()).isCloseTo(earlier.toInstant(), within(1, ChronoUnit.MILLIS));
    }

    @Value
    private static class TestProjection implements ReactionLastModifiedAt {
        String component;
        ZonedDateTime maxCreatedAt;
        ZonedDateTime maxModifiedAt;
    }
}
