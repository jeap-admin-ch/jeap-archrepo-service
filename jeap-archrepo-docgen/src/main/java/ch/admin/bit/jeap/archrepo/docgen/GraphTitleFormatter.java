package ch.admin.bit.jeap.archrepo.docgen;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

final class GraphTitleFormatter {

    private static final String DEFAULT_VARIANT = "Default";

    private GraphTitleFormatter() {
    }

    static String systemGraph(String systemName) {
        return format("System Graph", systemName);
    }

    static String serviceGraph(String serviceName) {
        return format("Service Graph", serviceName);
    }

    static String messageGraph(String messageTypeName, String variant) {
        String visibleVariant = DEFAULT_VARIANT.equals(trim(variant)) ? null : variant;
        return format("Message Graph", messageTypeName, visibleVariant);
    }

    private static String format(String... parts) {
        return Arrays.stream(parts)
                .map(GraphTitleFormatter::trim)
                .filter(Objects::nonNull)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(" - "));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
