package ch.admin.bit.jeap.archrepo.importers;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;

public interface ArchRepoImporter {

    default int getOrder() {
        return Integer.MAX_VALUE - 100;
    }

    void importIntoModel(ArchitectureModel architectureModel, String environment);
}
