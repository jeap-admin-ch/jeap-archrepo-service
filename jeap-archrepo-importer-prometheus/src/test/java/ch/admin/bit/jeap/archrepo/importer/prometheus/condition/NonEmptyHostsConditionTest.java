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
    public void testConditionWithValidProperty() {
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("prometheus-rhos.hosts[0].host")).thenReturn("dummy");

        when(annotatedTypeMetadata.getAnnotationAttributes(ConditionalOnNonEmptyHosts.class.getName()))
                .thenReturn(Map.of("propertyName", "prometheus-rhos.hosts"));

        boolean result = nonEmptyHostsCondition.matches(conditionContext, annotatedTypeMetadata);

        assertTrue(result);
    }

    @Test
    public void testConditionWithEmptyProperty() {
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("prometheus-rhos.hosts[0].host")).thenReturn(null);

        when(annotatedTypeMetadata.getAnnotationAttributes(ConditionalOnNonEmptyHosts.class.getName()))
                .thenReturn(Map.of("propertyName", "prometheus-rhos.hosts"));

        boolean result = nonEmptyHostsCondition.matches(conditionContext, annotatedTypeMetadata);

        assertFalse(result);
    }

    @Test
    public void testConditionWithMultipleIndexedProperties() {
        when(conditionContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("prometheus-rhos.hosts[0].host")).thenReturn("host1");
        when(environment.getProperty("prometheus-rhos.hosts[1].host")).thenReturn("host2");

        when(annotatedTypeMetadata.getAnnotationAttributes(ConditionalOnNonEmptyHosts.class.getName()))
                .thenReturn(Map.of("propertyName", "prometheus-rhos.hosts"));

        boolean result = nonEmptyHostsCondition.matches(conditionContext, annotatedTypeMetadata);

        assertTrue(result);
    }

    @Test
    public void testConditionWithNoHosts() {
        // Simulate the condition where no hosts are defined
        when(conditionContext.getEnvironment()).thenReturn(environment);

        when(environment.getProperty("prometheus-rhos.hosts[0].host")).thenReturn(null);  // Simulating no host defined

        when(annotatedTypeMetadata.getAnnotationAttributes(ConditionalOnNonEmptyHosts.class.getName()))
                .thenReturn(Map.of("propertyName", "prometheus-rhos.hosts"));

        boolean result = nonEmptyHostsCondition.matches(conditionContext, annotatedTypeMetadata);

        assertFalse(result);
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        nonEmptyHostsCondition = new NonEmptyHostsCondition();
    }

}