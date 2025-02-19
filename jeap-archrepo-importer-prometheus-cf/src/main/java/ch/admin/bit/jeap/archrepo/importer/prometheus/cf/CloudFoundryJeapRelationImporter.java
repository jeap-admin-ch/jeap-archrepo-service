package ch.admin.bit.jeap.archrepo.importer.prometheus.cf;

import ch.admin.bit.jeap.archrepo.importer.prometheus.ValidatedJeapRelationImporter;
import ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client.CloudFoundryPrometheusClient;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class CloudFoundryJeapRelationImporter implements ArchRepoImporter {

    private final CloudFoundryPrometheusClient cloudFoundryPrometheusClient;
    private final ValidatedJeapRelationImporter jeapRelationImporter;

    CloudFoundryJeapRelationImporter(CloudFoundryPrometheusClient cloudFoundryPrometheusClient) {
        this.cloudFoundryPrometheusClient = cloudFoundryPrometheusClient;
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
            cloudFoundryPrometheusClient.apiRelations()
                    .forEach(jeapRelation -> jeapRelationImporter.importJeapRelationFromPrometheus(model, jeapRelation));
        } catch (Exception ex) {
            log.warn("Failed to retrieve API relations from grafana", ex);
        }
    }
}
