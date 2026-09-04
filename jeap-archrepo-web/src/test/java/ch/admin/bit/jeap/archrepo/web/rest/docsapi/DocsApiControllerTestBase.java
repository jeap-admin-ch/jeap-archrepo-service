package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import ch.admin.bit.jeap.archrepo.web.PostgresIntegrationTestBase;

/**
 * The tokens the docs API tests authenticate with. The system name matches
 * {@code jeap.security.oauth2.resourceserver.system-name} in {@code application-test.yml}, because a semantic
 * role only matches when its system part does.
 */
abstract class DocsApiControllerTestBase extends PostgresIntegrationTestBase {

    protected static final String SYSTEM_NAME = "application-platform";

    protected static JeapAuthenticationToken tokenWithArchitectureModelRead() {
        return tokenWithRole(role(SYSTEM_NAME, "architecture-model", "read"));
    }

    /**
     * The same resource with another operation - proves the operation is checked, not only the resource.
     */
    protected static JeapAuthenticationToken tokenWithArchitectureModelWrite() {
        return tokenWithRole(role(SYSTEM_NAME, "architecture-model", "write"));
    }

    /**
     * The right role, issued for another system - proves the system part of the role is checked.
     */
    protected static JeapAuthenticationToken tokenOfAnotherSystem() {
        return tokenWithRole(role("another-system", "architecture-model", "read"));
    }

    protected static JeapAuthenticationToken tokenWithoutRoles() {
        return JeapAuthenticationTestTokenBuilder.create().build();
    }

    private static JeapAuthenticationToken tokenWithRole(SemanticApplicationRole role) {
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(role).build();
    }

    private static SemanticApplicationRole role(String system, String resource, String operation) {
        return SemanticApplicationRole.builder()
                .system(system)
                .resource(resource)
                .operation(operation)
                .build();
    }
}
