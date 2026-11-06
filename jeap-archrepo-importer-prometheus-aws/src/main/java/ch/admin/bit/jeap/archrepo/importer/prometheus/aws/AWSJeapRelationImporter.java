package ch.admin.bit.jeap.archrepo.importer.prometheus.aws;

import ch.admin.bit.jeap.archrepo.importer.prometheus.ValidatedJeapRelationImporter;
import ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client.AWSPrometheusClient;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class AWSJeapRelationImporter implements ArchRepoImporter {

    private final AWSPrometheusClient awsPrometheusClient;

    AWSJeapRelationImporter(AWSPrometheusClient awsPrometheusClient) {
        this.awsPrometheusClient = awsPrometheusClient;
    }

    @Override
    public int getOrder() {
        // This importer should run after AWSSystemComponentImporter but before PactBrokerImporter
        return Integer.MIN_VALUE + 100;
    }

    @Override
    public void importIntoModel(ArchitectureModel model, String environment) {
        ValidatedJeapRelationImporter jeapRelationImporter = new ValidatedJeapRelationImporter(log);
        try {
            awsPrometheusClient.apiRelations(environment)
                    .forEach(jeapRelation -> jeapRelationImporter.importJeapRelationFromPrometheus(model, jeapRelation));
        } catch (Exception ex) {
            log.warn("Failed to retrieve API relations from prometheus", ex);
        }
    }

}
