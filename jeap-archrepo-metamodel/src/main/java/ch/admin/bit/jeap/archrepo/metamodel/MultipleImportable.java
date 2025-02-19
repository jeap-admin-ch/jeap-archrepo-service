package ch.admin.bit.jeap.archrepo.metamodel;

import java.util.List;
import java.util.Set;

public interface MultipleImportable {

    Set<Importer> getImporters();

    void addImporter(Importer importer);

    static <T extends MultipleImportable> List<T> filterByImportedOnlyByImporter(List<T> importables, Importer importer) {
        return importables.stream()
                .filter(importable -> Set.of(importer).equals(importable.getImporters()))
                .toList();
    }
}
