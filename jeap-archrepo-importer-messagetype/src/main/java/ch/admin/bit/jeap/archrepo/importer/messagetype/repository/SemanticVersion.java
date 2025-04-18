package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;

@Getter
@EqualsAndHashCode
public class SemanticVersion implements Comparable<SemanticVersion> {
    private final int major;
    private final int minor;
    private final int bugfix;

    public SemanticVersion(int major, int minor, int bugfix) {
        this.major = major;
        this.minor = minor;
        this.bugfix = bugfix;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + bugfix;
    }

    public static SemanticVersion parse(String version) {
        try {
            Integer[] parts = Arrays.stream(version.split("\\."))
                    .map(Integer::parseInt)
                    .toArray(Integer[]::new);
            return new SemanticVersion(parts[0], parts[1], parts[2]);
        } catch (Exception e) {
            throw new RuntimeException("Cannot convert " + version + " to a semantic version", e);
        }
    }

    @Override
    public int compareTo(SemanticVersion other) {
        return Comparator.comparing(SemanticVersion::getMajor)
                .thenComparing(SemanticVersion::getMinor)
                .thenComparing(SemanticVersion::getBugfix)
                .compare(this, other);
    }
}
