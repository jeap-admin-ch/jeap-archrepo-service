package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.web.ArchRepoApplication;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The evidence for the story's "no existing functionality is removed" criterion.
 * <p>
 * Adding {@code /docs-api} must not change {@code /api/**} or {@code /external-api/**} - not their paths, not
 * their authentication and not their error format. The two ways that could break silently are a
 * {@code @ControllerAdvice} matching wider than intended and a filter-chain matcher that shifts; neither is
 * visible in a {@code @WebMvcTest} of a single controller.
 */
@SpringBootTest(classes = ArchRepoApplication.class, properties = "archrepo-config.environment=dev")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(DisableJeapPermitAllSecurityConfiguration.class)
class ExistingApiUnchangedIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicModelEndpointsStayPublic() throws Exception {
        mockMvc.perform(get("/api/model")).andExpect(status().isOk());
        mockMvc.perform(get("/api/model/rest-api-relation-without-pact")).andExpect(status().isOk());
        mockMvc.perform(get("/api/model/system-components-without-open-api-spec")).andExpect(status().isOk());
    }

    @Test
    void publicArtifactEndpointsStayPublic() throws Exception {
        mockMvc.perform(get("/api/openapi/versions")).andExpect(status().isOk());
        mockMvc.perform(get("/api/dbschemas/versions")).andExpect(status().isOk());
    }

    @Test
    void theExternalApiStillRequiresItsOwnRole() throws Exception {
        mockMvc.perform(get("/external-api/dbschemas").param("systemComponentName", "any"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void errorsOfTheExistingApiAreStillPlainText() throws Exception {
        // The docs API answers problem+json; the advice that does so is scoped to its package, and the existing
        // endpoints must keep the format their consumers already parse.
        mockMvc.perform(get("/api/model/no-such-system/relations"))
                .andExpect(status().isNotFound())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    if (contentType != null && contentType.contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE)) {
                        throw new AssertionError("An /api/** error must not become a problem document, but was "
                                                 + contentType);
                    }
                });
    }

    @Test
    void aWriteEndpointOfTheExistingApiStillNeedsBasicAuth() throws Exception {
        mockMvc.perform(get("/api/management/anything")).andExpect(status().isUnauthorized());
    }

    @Test
    void theModelPayloadIsUnchanged() throws Exception {
        // /api/model is under Pact contract; the docs API deliberately does not reuse its DTOs
        mockMvc.perform(get("/api/model"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("systems")));
    }
}
