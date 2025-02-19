package ch.admin.bit.jeap.archrepo.importers;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;

public interface ArchRepoImporter {

    default int getOrder() {
        return Integer.MAX_VALUE;
    }

    void importIntoModel(ArchitectureModel architectureModel);
}
