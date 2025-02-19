package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import lombok.Value;

import java.util.HashSet;
import java.util.Set;

@Value
class GeneratorContext {
    ArchitectureModel model;
    String rootPageId;
    Set<String> generatedPageIds = new HashSet<>();

    void addGeneratedPageIds(String... pageIds) {
        generatedPageIds.addAll(Set.of(pageIds));
    }
}
