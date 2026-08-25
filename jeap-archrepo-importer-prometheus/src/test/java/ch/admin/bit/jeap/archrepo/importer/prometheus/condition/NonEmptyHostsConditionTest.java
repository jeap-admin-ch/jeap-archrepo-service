package ch.admin.bit.jeap.archrepo.importer.prometheus.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class NonEmptyHostsConditionTest {

    @Mock
    private ConditionContext conditionContext;

    @Mock
    private Environment environment;

    @Mock
    private AnnotatedTypeMetadata annotatedTypeMetadata;

    private NonEmptyHostsCondition nonEmptyHostsCondition;

    @Test
    void testConditionWithValidProperty() {
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("prometheus-rhos.hosts[0].host")).thenReturn("dummy");

        when(annotatedTypeMetadata.getAnnotationAttributes(ConditionalOnNonEmptyHosts.class.getName()))
                .thenReturn(Map.of("propertyName", "prometheus-rhos.hosts"));

        boolean result = nonEmptyHostsCondition.matches(conditionContext, annotatedTypeMetadata);

        assertTrue(result);
    }

    @Test
    void testConditionWithEmptyProperty() {
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("prometheus-rhos.hosts[0].host")).thenReturn(null);

        when(annotatedTypeMetadata.getAnnotationAttributes(ConditionalOnNonEmptyHosts.class.getName()))
                .thenReturn(Map.of("propertyName", "prometheus-rhos.hosts"));

        boolean result = nonEmptyHostsCondition.matches(conditionContext, annotatedTypeMetadata);

        assertFalse(result);
    }

    @Test
    void testConditionWithMultipleIndexedProperties() {
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("prometheus-rhos.hosts[0].host")).thenReturn("host1");
        when(environment.getProperty("prometheus-rhos.hosts[1].host")).thenReturn("host2");

        when(annotatedTypeMetadata.getAnnotationAttributes(ConditionalOnNonEmptyHosts.class.getName()))
                .thenReturn(Map.of("propertyName", "prometheus-rhos.hosts"));

        boolean result = nonEmptyHostsCondition.matches(conditionContext, annotatedTypeMetadata);

        assertTrue(result);
    }

    @Test
    void testConditionWithoutAnnotationAttributes() {
        // getAnnotationAttributes() is declared nullable; the condition must answer false rather than throw
        when(annotatedTypeMetadata.getAnnotationAttributes(ConditionalOnNonEmptyHosts.class.getName()))
                .thenReturn(null);

        boolean result = nonEmptyHostsCondition.matches(conditionContext, annotatedTypeMetadata);

        assertFalse(result);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        nonEmptyHostsCondition = new NonEmptyHostsCondition();
    }

}