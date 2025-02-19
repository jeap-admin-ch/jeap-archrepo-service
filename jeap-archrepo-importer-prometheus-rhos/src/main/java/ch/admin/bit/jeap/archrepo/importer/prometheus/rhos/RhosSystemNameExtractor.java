package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RhosSystemNameExtractor {

    private static final String REGEX = "^[^-]+-([^-]+)-"; // a namespace name looks like this: bit-jme-d or bit-jme-r, where jme is the system
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    Optional<String> extractSystemName(String namespace) {
        Matcher matcher = PATTERN.matcher(namespace);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

}
