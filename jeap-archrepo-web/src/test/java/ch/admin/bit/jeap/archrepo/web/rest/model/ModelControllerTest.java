package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentType;
import ch.admin.bit.jeap.archrepo.persistence.ArchitectureModelRepository;
import ch.admin.bit.jeap.archrepo.web.config.WebSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ModelController.class, WebSecurityConfig.class})
@Import(ArchRepoWebTestConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModelControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    ArchitectureModelRepository architectureModelRepository;

    @Test
    void getModel() throws Exception {
        when(architectureModelRepository.load()).thenReturn(ModelStub.createSimpleModel());

        String json = mockMvc.perform(get("/api/model")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ModelDto dto = objectMapper.readValue(json, ModelDto.class);
        SystemDto systemDto = dto.getSystems().getFirst();
        assertEquals(ModelStub.SYSTEM, systemDto.getName());
        SystemComponentDto systemComponentDto = systemDto.getSystemComponents().getFirst();
        assertEquals(ModelStub.COMPONENT, systemComponentDto.getName());
        assertEquals(Importer.GRAFANA, systemComponentDto.getImporter());
        assertEquals("Lagrev", systemComponentDto.getOwnedBy());
        assertEquals("desc", systemComponentDto.getDescription());
        assertEquals(SystemComponentType.BACKEND_SERVICE, systemComponentDto.getType());
    }

    @Test
    void getModelRelations() throws Exception {
        when(architectureModelRepository.load()).thenReturn(ModelStub.createSimpleModelWithOneRestApiRelation());

        String json = mockMvc.perform(get("/api/model/system/relations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        RelationDto[] relations = objectMapper.readValue(json, RelationDto[].class);
        assertThat(relations)
                .hasSize(1)
                .allMatch(r -> r.getPath().equals("/api/foo"));
    }

    @Test
    void getModelRelationsInexistantSystem() throws Exception {
        when(architectureModelRepository.load()).thenReturn(ModelStub.createSimpleModelWithOneRestApiRelation());

        mockMvc.perform(get("/api/model/badsystem/relations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSystemComponentsWithoutOpenApiSpec() throws Exception {
        when(architectureModelRepository.load()).thenReturn(ModelStub.createSimpleModelWithOneRestApiRelation());

        String json = mockMvc.perform(get("/api/model/system-components-without-open-api-spec")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String[] services = objectMapper.readValue(json, String[].class);
        assertThat(services)
                .hasSize(2)
                .containsExactlyInAnyOrder("consumer", "provider");
    }
}
