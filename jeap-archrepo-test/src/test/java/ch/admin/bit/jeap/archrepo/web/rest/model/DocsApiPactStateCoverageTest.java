package ch.admin.bit.jeap.archrepo.web.rest.model;

import au.com.dius.pact.provider.junitsupport.State;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * That every resource of the docs API has a provider state, and every provider state a resource.
 * <p>
 * A pact is verified against a provider that has been put into the state the interaction names, so a resource
 * without a state cannot be under contract at all - and nothing else would say so: the consumer lives in
 * another repository, its pacts arrive weeks later, and {@code @IgnoreNoPactsToVerify} means a verification
 * with no pact for a resource passes quietly. This is the counterpart of {@code DocsApiRoleCoverageTest}, which
 * fails the build when a handler appears without the semantic role.
 * <p>
 * The routes are read from the controllers rather than listed here, so adding a tenth resource fails this test
 * until {@link PactProviderTestBase#DOCS_API_ROUTE_STATES} names the state that serves it.
 */
class DocsApiPactStateCoverageTest {

    private static final String DOCS_API_PACKAGE = "ch.admin.bit.jeap.archrepo.web.rest.docsapi";
    private static final String DOCS_API_PREFIX = "/docs-api";

    @Test
    void everyDocsApiRoute_hasAProviderState() {
        assertThat(declaredDocsApiRoutes())
                .describedAs("every route of the docs API needs a provider state in "
                             + "PactProviderTestBase.DOCS_API_ROUTE_STATES, or a consumer cannot put the "
                             + "provider into the state its interaction needs")
                .isEqualTo(new TreeSet<>(PactProviderTestBase.DOCS_API_ROUTE_STATES.keySet()));
    }

    @Test
    void everyMappedState_isDeclaredOnTheProviderTestBase() {
        Set<String> declared = declaredStateNames();

        assertThat(declared)
                .describedAs("the states the routes are mapped to have to exist as @State methods")
                .containsAll(PactProviderTestBase.DOCS_API_ROUTE_STATES.values());
    }

    /**
     * The routes the controllers of the docs API declare, class-level mapping and method-level mapping joined.
     * Read through the merged {@link RequestMapping} so that a resource added with any of the shortcut
     * annotations is seen.
     */
    private static Set<String> declaredDocsApiRoutes() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<String> routes = new TreeSet<>();
        scanner.findCandidateComponents(DOCS_API_PACKAGE).forEach(candidate -> {
            Class<?> controller = resolve(candidate.getBeanClassName());
            String prefix = firstPathOf(AnnotatedElementUtils.findMergedAnnotation(controller,
                    RequestMapping.class));
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method,
                        RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                String route = prefix + firstPathOf(mapping);
                if (route.startsWith(DOCS_API_PREFIX)) {
                    routes.add(route);
                }
            }
        });
        assertThat(routes).describedAs("the docs API controllers were not found - has the package moved?")
                .isNotEmpty();
        return routes;
    }

    private static Set<String> declaredStateNames() {
        Set<String> names = new TreeSet<>();
        for (Method method : PactProviderTestBase.class.getDeclaredMethods()) {
            State state = method.getAnnotation(State.class);
            if (state != null) {
                names.addAll(Arrays.asList(state.value()));
            }
        }
        return names;
    }

    /**
     * The one path of a mapping. Every resource of the docs API declares exactly one - a second one would be
     * the same resource under two names, which the consumer's URLs could not both be, so it is not something
     * to support quietly here.
     */
    private static String firstPathOf(RequestMapping mapping) {
        if (mapping == null || mapping.value().length == 0) {
            return "";
        }
        assertThat(mapping.value()).describedAs("a docs API mapping declares more than one path")
                .hasSize(1);
        return mapping.value()[0];
    }

    private static Class<?> resolve(String className) {
        try {
            return ClassUtils.forName(className, DocsApiPactStateCoverageTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("The scanner found %s but it cannot be loaded.".formatted(className), e);
        }
    }
}
