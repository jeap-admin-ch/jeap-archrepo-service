package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Wiring of the docs API, and the check that the instance can actually authorize it.
 * <p>
 * The API is always active - it is part of what the arch repo is, not a feature an instance opts into.
 */
@Configuration
class DocsApiConfiguration {

    private static final String SYSTEM_NAME_PROPERTY = "jeap.security.oauth2.resourceserver.system-name";

    /**
     * Every docs API resource is authorized with the semantic role {@code architecture-model} / {@code read}. The
     * jEAP security starter only activates semantic authorization - and with it the two-argument
     * {@code hasRole(resource, operation)} expression - when the system name is configured. Without it every
     * resource would fail its authorization check at runtime, so the instance is stopped here instead: a
     * configuration error belongs in the deployment, not in the first request.
     */
    DocsApiConfiguration(@Value("${" + SYSTEM_NAME_PROPERTY + ":}") String systemName) {
        if (!StringUtils.hasText(systemName)) {
            throw new IllegalStateException(
                    "'" + SYSTEM_NAME_PROPERTY + "' is not set. The docs API authorizes every resource with a " +
                    "semantic role, and the jEAP security starter only activates semantic authorization when the " +
                    "system name is configured. Configure it.");
        }
    }

    @Bean
    GroupedOpenApi docsApi() {
        return GroupedOpenApi.builder()
                .group("Architecture Repository Docs API")
                .pathsToMatch(DocsApiPaths.DOCS_API + "/**")
                .packagesToScan(DocsApiConfiguration.class.getPackageName())
                .build();
    }
}
