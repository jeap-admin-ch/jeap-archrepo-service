package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;

import java.time.ZonedDateTime;
import java.util.List;

class ModelStub {

    static final String SYSTEM = "system";
    static final String ALIAS = "sysalias";
    static final String COMPONENT = "system-context-service";

    static ArchitectureModel createSimpleModel() {
        BackendService backendService = BackendService.builder()
                .name(COMPONENT)
                .importer(Importer.GRAFANA)
                .ownedBy(Team.builder().name("Lagrev").build())
                .description("desc")
                .build();
        System system = System.builder()
                .name(SYSTEM)
                .aliases(List.of(ALIAS))
                .systemComponents(List.of(backendService))
                .defaultOwner(Team.builder().name("team").build())
                .build();
        return ArchitectureModel.builder()
                .systems(List.of(system))
                .build();
    }

    static ArchitectureModel createSimpleModelWithOneRestApiRelation() {
        BackendService providerService = BackendService.builder()
                .name("provider")
                .importer(Importer.GRAFANA)
                .build();
        BackendService consumerService = BackendService.builder()
                .name("consumer")
                .importer(Importer.GRAFANA)
                .build();
        System system = System.builder()
                .name(SYSTEM)
                .aliases(List.of(ALIAS))
                .systemComponents(List.of(providerService, consumerService))
                .build();
        RestApi restApi = RestApi.builder()
                .provider(providerService)
                .method("GET")
                .path("/api/foo")
                .importer(Importer.GRAFANA)
                .build();
        RestApiRelation restApiRelation = RestApiRelation.builder()
                .consumerName("consumer")
                .restApi(restApi)
                .lastSeen(ZonedDateTime.now())
                .importer(Importer.GRAFANA)
                .build();
        system.addRelation(restApiRelation);
        RestApi restApiDeleted = RestApi.builder()
                .provider(providerService)
                .method("PUT")
                .path("/api/foo/bar")
                .importer(Importer.GRAFANA)
                .build();
        RestApiRelation restApiRelationDeleted = RestApiRelation.builder()
                .consumerName("consumer")
                .restApi(restApiDeleted)
                .lastSeen(ZonedDateTime.now())
                .importer(Importer.GRAFANA)
                .build();
        restApiRelationDeleted.markDeleted();
        system.addRelation(restApiRelationDeleted);

        return ArchitectureModel.builder()
                .systems(List.of(system))
                .build();
    }
}
