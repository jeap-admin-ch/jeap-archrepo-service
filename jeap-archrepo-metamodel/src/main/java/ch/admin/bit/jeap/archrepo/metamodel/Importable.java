package ch.admin.bit.jeap.archrepo.metamodel;

import java.util.List;

public interface Importable {

    Importer getImporter();

    void setImporter(Importer importer);

    static <T extends Importable> List<T> filterByImporter(List<T> importables, Importer importer) {
        return importables.stream()
                .filter(importable -> importer == importable.getImporter())
                .toList();
    }

}
