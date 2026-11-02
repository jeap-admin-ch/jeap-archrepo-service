package ch.admin.bit.jeap.archrepo.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * The case-insensitive identity rules for system and team names and system aliases. Applied to the configured
 * declaration when it is bound ({@link ArchRepoModelProperties}), and to the stored model before onboarding
 * changes anything ({@code ModelOnboardingService}).
 */
public final class ModelNames {

    private ModelNames() {
    }

    public static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    /**
     * The entries by case-insensitive name, in their original order. Two entries whose names differ only in case
     * are ambiguous and rejected rather than letting the order decide which one wins.
     */
    public static <T> Map<String, T> index(List<T> entries, Function<T, String> name, String kind) {
        Map<String, T> result = new LinkedHashMap<>();
        for (T entry : entries) {
            T previous = result.putIfAbsent(key(name.apply(entry)), entry);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate %s names '%s' and '%s'"
                        .formatted(kind, name.apply(previous), name.apply(entry)));
            }
        }
        return result;
    }

    /**
     * Names and aliases share one case-insensitive namespace: an importer resolves a system by either, so a value
     * claimed twice - as two names, a name and an alias, or two aliases - would make that lookup ambiguous.
     */
    public static <T> void validateNamespace(List<T> entries, Function<T, String> name,
                                             Function<T, List<String>> aliases, String kind) {
        Map<String, String> namespace = new LinkedHashMap<>();
        entries.forEach(entry -> claim(namespace, name.apply(entry), name.apply(entry), kind));
        entries.forEach(entry -> aliases.apply(entry)
                .forEach(alias -> claim(namespace, alias, name.apply(entry), kind)));
    }

    private static void claim(Map<String, String> namespace, String value, String system, String kind) {
        String previous = namespace.putIfAbsent(key(value), system);
        if (previous != null) {
            throw new IllegalArgumentException("Conflicting %s name/alias '%s' on systems '%s' and '%s'"
                    .formatted(kind, value, previous, system));
        }
    }
}
