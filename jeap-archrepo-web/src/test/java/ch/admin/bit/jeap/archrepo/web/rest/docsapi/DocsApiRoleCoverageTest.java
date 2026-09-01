package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the rule that <em>every</em> resource of the docs API is behind the semantic role: a role on all but
 * one resource is worse than none, because it reads as protected.
 * <p>
 * A handler method added without {@link RequiresArchitectureModelRead} fails the build here, naming the method,
 * instead of being discovered in production.
 */
class DocsApiRoleCoverageTest {

    private static final String PACKAGE = DocsApiRoleCoverageTest.class.getPackageName();

    @Test
    void everyDocsApiHandlerMethodRequiresTheArchitectureModelReadRole() {
        List<String> unprotected = new ArrayList<>();

        for (Class<?> controller : docsApiControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                if (isHandlerMethod(method) && !method.isAnnotationPresent(RequiresArchitectureModelRead.class)) {
                    unprotected.add(controller.getSimpleName() + "." + method.getName());
                }
            }
        }

        assertThat(unprotected)
                .withFailMessage("Every docs API handler method must be annotated with " +
                                 "@RequiresArchitectureModelRead, these are not: %s", unprotected)
                .isEmpty();
    }

    @Test
    void theControllersAreFound() {
        // Guards the guard: a scan that silently finds nothing would make the test above pass for the wrong reason
        assertThat(docsApiControllers())
                .extracting(Class::getSimpleName)
                .containsExactlyInAnyOrder("SystemsController", "ComponentArtifactController",
                        "ArtifactIndexController", "MessageTypesController");
    }

    @Test
    void theRoleAnnotationCarriesTheExpectedExpression() {
        PreAuthorize preAuthorize = RequiresArchitectureModelRead.class.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('architecture-model', 'read')");
    }

    private static boolean isHandlerMethod(Method method) {
        return method.isAnnotationPresent(RequestMapping.class) || isMappedByAMetaAnnotation(method);
    }

    private static boolean isMappedByAMetaAnnotation(Method method) {
        return Arrays.stream(method.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().isAnnotationPresent(RequestMapping.class));
    }

    private static List<Class<?>> docsApiControllers() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, new StandardEnvironment());
        scanner.setResourceLoader(new PathMatchingResourcePatternResolver());
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> controllers = new ArrayList<>();
        for (var definition : scanner.findCandidateComponents(PACKAGE)) {
            try {
                controllers.add(Class.forName(definition.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return controllers;
    }
}
