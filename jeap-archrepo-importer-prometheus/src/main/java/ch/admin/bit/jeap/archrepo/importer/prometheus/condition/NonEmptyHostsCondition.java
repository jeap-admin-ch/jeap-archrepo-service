package ch.admin.bit.jeap.archrepo.importer.prometheus.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class NonEmptyHostsCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String propertyName = (String) metadata.getAnnotationAttributes(ConditionalOnNonEmptyHosts.class.getName()).get("propertyName");

        int index = 0;
        while (true) {
            String indexedHost = String.format("%s[%d].host", propertyName, index);
            String value = context.getEnvironment().getProperty(indexedHost);
            if (value != null && !value.isEmpty()) {
                return true; // Found at least one non-empty entry
            }

            String nextIndexed = String.format("%s[%d]", propertyName, index + 1);
            if (context.getEnvironment().getProperty(nextIndexed) == null) {
                break; // No more indexed properties to check
            }
            index++;
        }

        return false;
    }

}