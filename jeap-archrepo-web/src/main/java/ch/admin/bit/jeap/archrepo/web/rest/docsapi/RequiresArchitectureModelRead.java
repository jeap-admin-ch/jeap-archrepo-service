package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the semantic role {@code <system-name>_@architecture-model_#read}.
 * <p>
 * Every resource of the docs API carries this annotation - there is no public and no merely authenticated
 * resource under {@link DocsApiPaths#DOCS_API}. {@code DocsApiRoleCoverageTest} fails the build if a handler
 * method is added without it.
 * <p>
 * The role only resolves when semantic authorization is active, which the jEAP security starter ties to
 * {@code jeap.security.oauth2.resourceserver.system-name}. {@link DocsApiConfiguration} fails the startup when
 * that property is missing.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@PreAuthorize("hasRole('architecture-model', 'read')")
public @interface RequiresArchitectureModelRead {
}
