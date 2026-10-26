package ch.admin.bit.jeap.archrepo.web.rest.openapi;

import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.persistence.TeamRepository;
import ch.admin.bit.jeap.archrepo.web.ArchRepoApplication;
import ch.admin.bit.jeap.archrepo.web.PostgresIntegrationTestBase;
import ch.admin.bit.jeap.archrepo.web.service.UpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The REST APIs read from an uploaded OpenAPI spec must still be served by {@code /rest-apis} after the scheduled
 * model update has run. Nobody needs to consume them for that: the model update's cleanup used to remove every
 * REST API without a relation, which left the list empty for any provider whose endpoints no known consumer calls.
 * <p>
 * Deliberately not {@code @Transactional}: the upload, the model update and the read each run in a transaction of
 * their own, as they do in production, so that what the cleanup deletes is actually gone from the database.
 */
@SpringBootTest(classes = ArchRepoApplication.class, properties = "archrepo-config.environment=dev")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiRestApisSurviveModelUpdateIT extends PostgresIntegrationTestBase {

    private static final String SPEC = """
            {
              "openapi": "3.0.1",
              "servers": [{"url": "https://provider.example.com"}],
              "paths": {
                "/api/documents": {"get": {}, "post": {}}
              }
            }
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UpdateService updateService;
    @Autowired
    private SystemRepository systemRepository;
    @Autowired
    private TeamRepository teamRepository;

    // No importer may contribute a relation onto the uploaded REST APIs, or the cleanup would keep them anyway
    @MockitoBean
    private ArchRepoImporter importer;

    @Test
    void restApisFromAnUploadedSpecSurviveTheModelUpdate() throws Exception {
        // Committed rather than rolled back, so the names are unique for the rest of the suite
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String provider = "openapi-provider-" + suffix;
        seedSystemWithComponent("openapi-system-" + suffix, provider, suffix);

        mockMvc.perform(multipart("/api/openapi/" + provider + "?version=1.0.0")
                        .file(new MockMultipartFile("file", "openapi.json", MediaType.APPLICATION_JSON_VALUE,
                                SPEC.getBytes(StandardCharsets.UTF_8)))
                        .with(httpBasic("archrepo-admin", "secret")))
                .andExpect(status().isCreated());

        updateService.updateModel();

        mockMvc.perform(get("/api/openapi/" + provider + "/rest-apis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverUrl").value("https://provider.example.com"))
                .andExpect(jsonPath("$.restApis", hasSize(2)))
                .andExpect(jsonPath("$.restApis[*].method", containsInAnyOrder("GET", "POST")));
    }

    private void seedSystemWithComponent(String systemName, String componentName, String suffix) {
        Team team = teamRepository.save(Team.builder().name("Team " + suffix).build());
        SystemComponent component = BackendService.builder()
                .id(UUID.randomUUID()).name(componentName).ownedBy(team).build();
        ReflectionTestUtils.setField(component, "createdAt", ZonedDateTime.now());
        systemRepository.saveAndFlush(System.builder()
                .name(systemName)
                .defaultOwner(team)
                .systemComponents(List.of(component))
                .build());
    }
}
