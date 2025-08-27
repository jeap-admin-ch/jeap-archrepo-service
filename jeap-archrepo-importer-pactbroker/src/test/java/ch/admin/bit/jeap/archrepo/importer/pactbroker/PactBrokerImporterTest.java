package ch.admin.bit.jeap.archrepo.importer.pactbroker;

import ch.admin.bit.jeap.archrepo.importer.pactbroker.client.PactBrokerClient;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(PactStubBrokerExtension.class)
class PactBrokerImporterTest {

    private ArchitectureModel model;
    private System shared;

    @Test
    void importIntoModel(PactBrokerClient pactBrokerClient) {
        PactBrokerImporter importer = new PactBrokerImporter(pactBrokerClient);

        importer.importIntoModel(model, "ref");

        assertEquals(3, shared.getRestApis().size());
        assertEquals("/api/tasks/748fda55-7411-44ba-b8f1-f84f8fc5d50e", shared.getRestApis().get(0).getPath());
        assertEquals("PUT", shared.getRestApis().get(0).getMethod());
        assertEquals("/api/tasks/748fda55-7411-44ba-b8f1-f84f8fc5d50e/state", shared.getRestApis().get(1).getPath());
        assertEquals("PUT", shared.getRestApis().get(1).getMethod());
        assertEquals("/api/task-configs", shared.getRestApis().get(2).getPath());
        assertEquals("PUT", shared.getRestApis().get(2).getMethod());

        assertEquals(3, shared.getRelations().size());
    }

    @BeforeEach
    void buildModel() {
        System jeap = System.builder()
                .name("jeap")
                .systemComponents(List.of(BackendService.builder()
                        .name("jeap-error-handling-service")
                        .build()))
                .build();
        System jme = System.builder()
                .name("jme")
                .systemComponents(List.of(
                        BackendService.builder().name("jme-cdc-segregatedProvider-service").build(),
                        BackendService.builder().name("jme-cdc-consumer-service").build())
                )
                .build();
        shared = System.builder()
                .name("agir")
                .systemComponents(List.of(BackendService.builder()
                        .name("shared-agir-service")
                        .build()))
                .build();

        model = ArchitectureModel.builder()
                .systems(asList(jeap, jme, shared))
                .build();
    }
}
