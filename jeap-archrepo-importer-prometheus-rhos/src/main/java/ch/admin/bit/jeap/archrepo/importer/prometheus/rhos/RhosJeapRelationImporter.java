package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos;

import ch.admin.bit.jeap.archrepo.importer.prometheus.ValidatedJeapRelationImporter;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.RhosGrafanaClient;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class RhosJeapRelationImporter implements ArchRepoImporter {

    private final RhosGrafanaClient rhosGrafanaClient;
    private final ValidatedJeapRelationImporter jeapRelationImporter;

    RhosJeapRelationImporter(RhosGrafanaClient rhosGrafanaClient) {
        this.rhosGrafanaClient = rhosGrafanaClient;
        jeapRelationImporter = new ValidatedJeapRelationImporter(log);
    }

    @Override
    public int getOrder() {
        // GrafanaJeapRelationImporter should run after GrafanaSystemComponentImporter but before PactBrokerImporter
        return Integer.MIN_VALUE + 100;
    }

    @Override
    public void importIntoModel(ArchitectureModel model) {
        try {
            rhosGrafanaClient.apiRelations()
                    .forEach(jeapRelation -> jeapRelationImporter.importJeapRelationFromPrometheus(model, jeapRelation));
        } catch (Exception ex) {
            log.warn("Failed to retrieve API relations from grafana", ex);
        }
    }

}
