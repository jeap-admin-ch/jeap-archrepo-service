package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.web.ArchRepoApplication;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * That every resource of the docs API is behind the semantic role, on the running application.
 * <p>
 * Parameterized over <em>every</em> route rather than a sample: a resource added later without the role has to
 * fail here, not be missed because the test happened to cover a different one. {@code DocsApiRoleCoverageTest}
 * proves the annotation is present; this proves the annotation and the filter chain take effect.
 */
@SpringBootTest(classes = ArchRepoApplication.class, properties = "archrepo-config.environment=dev")
@AutoConfigureMockMvc
@ActiveProfiles("test")
// Without this the permit-all chain of jeap-spring-boot-security-starter-test matches /** and shadows production
// security, and every assertion below would pass while proving nothing.
@Import(DisableJeapPermitAllSecurityConfiguration.class)
class DocsApiSecurityIT extends DocsApiControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Every route of the docs API. The data behind them is irrelevant here - what matters is that authorization
     * is decided before the handler runs, so an empty model still yields 401/403/2xx-or-404, never 200 for an
     * unauthorized caller.
     */
    static Stream<String> everyDocsApiRoute() {
        return Stream.of(
                DocsApiPaths.SYSTEMS,
                DocsApiPaths.SYSTEMS + "/wvs",
                DocsApiPaths.SYSTEMS + "/wvs/messages",
                DocsApiPaths.openApiContentPath("wvs", "wvs-foo-bar-service"),
                DocsApiPaths.databaseSchemaContentPath("wvs", "wvs-foo-bar-service"),
                DocsApiPaths.OPENAPI_SPECS,
                DocsApiPaths.DATABASE_SCHEMAS);
    }

    @ParameterizedTest(name = "{0} without a token is 401")
    @MethodSource("everyDocsApiRoute")
    void withoutAToken_isUnauthorized(String route) throws Exception {
        mockMvc.perform(get(route)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} with the basic credentials of /api is 401")
    @MethodSource("everyDocsApiRoute")
    void withTheApiBasicCredentials_isUnauthorized(String route) throws Exception {
        // The docs API is not on the basic-auth chain that serves /api/**; credentials that open /api do nothing
        mockMvc.perform(get(route).with(httpBasic("api", "secret")))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} with a token without roles is 403")
    @MethodSource("everyDocsApiRoute")
    void withoutTheRole_isForbidden(String route) throws Exception {
        mockMvc.perform(get(route).with(authentication(tokenWithoutRoles())))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} with architecture-model/write is 403")
    @MethodSource("everyDocsApiRoute")
    void withTheWrongOperation_isForbidden(String route) throws Exception {
        // The operation is part of the role, not decoration
        mockMvc.perform(get(route).with(authentication(tokenWithArchitectureModelWrite())))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} with the role of another system is 403")
    @MethodSource("everyDocsApiRoute")
    void withTheRoleOfAnotherSystem_isForbidden(String route) throws Exception {
        // The system part of the role is checked too, so a token issued for another landscape does not pass
        mockMvc.perform(get(route).with(authentication(tokenOfAnotherSystem())))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} with architecture-model/read passes authorization")
    @MethodSource("everyDocsApiRoute")
    void withTheRole_isAuthorized(String route) throws Exception {
        // The database is empty, so a resource addressing a system answers 404 and an index answers 200. Asserted
        // as "2xx or 404" rather than "not 401/403": the looser form would let a 500 pass as authorized.
        mockMvc.perform(get(route).with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (!(status == 404 || (status >= 200 && status < 300))) {
                        throw new AssertionError("Expected " + route + " to answer 2xx or 404 once authorized, "
                                                 + "but was " + status);
                    }
                });
    }
}
