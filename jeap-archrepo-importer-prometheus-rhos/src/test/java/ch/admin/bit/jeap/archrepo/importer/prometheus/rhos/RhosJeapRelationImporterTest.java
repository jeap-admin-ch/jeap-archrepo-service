package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.JeapRelation;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.RhosGrafanaClient;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.MobileApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RhosJeapRelationImporterTest {

    private ArchitectureModel model;
    private ch.admin.bit.jeap.archrepo.metamodel.System shared;

    @InjectMocks
    private RhosJeapRelationImporter importer;

    @Mock
    private RhosGrafanaClient grafanaClient;

    @Test
    void importIntoModel_newRelations_relationsImportedInModel() {

        //given
        List<JeapRelation> jeapRelations = List.of(
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "PUT", "http", "/api/tasks/{param}"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "GET", "http", "/api/tasks/{param}/state"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "POST", "http", "/api/tasks/{param}/state"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "GET", "http", "/api/task/{param}/content"));
        when(grafanaClient.apiRelations()).thenReturn(jeapRelations);

        //when
        importer.importIntoModel(model);

        //then
        assertEquals(4, shared.getRestApis().size());

        assertEquals("PUT", shared.getRestApis().get(0).getMethod());
        assertEquals("/api/tasks/{param}", shared.getRestApis().get(0).getPath());

        assertEquals("GET", shared.getRestApis().get(1).getMethod());
        assertEquals("/api/tasks/{param}/state", shared.getRestApis().get(1).getPath());

        assertEquals("POST", shared.getRestApis().get(2).getMethod());
        assertEquals("/api/tasks/{param}/state", shared.getRestApis().get(2).getPath());

        assertEquals("GET", shared.getRestApis().get(3).getMethod());
        assertEquals("/api/task/{param}/content", shared.getRestApis().get(3).getPath());

        assertEquals(4, shared.getRelations().size());
    }

    @Test
    void importIntoModel_newRelations_shouldIgnoreUninteresingDataPoints() {

        //given
        List<JeapRelation> jeapRelations = List.of(
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "PUT", "http", "/api/tasks/{param}"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "OPTIONS", "http", "/api/tasks/{param}"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "GET", "http", "/**"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "HEAD", "http", "/api/tasks/{param}"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "TRACE", "http", "/api/tasks/{param}"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "GET", "http", "/actuator/health"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "GET", "http", "/foo/actuator/bar"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "GET", "http", "/foo/;zapscan"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", "GET", "http", "/foo/%22"),
                new JeapRelation("shared-agir-service", "jeap-error-handling-service", null, "other", "MyEvent"));
        when(grafanaClient.apiRelations()).thenReturn(jeapRelations);

        //when
        importer.importIntoModel(model);

        //then
        assertEquals(1, shared.getRelations().size());
    }

    @ParameterizedTest
    @CsvSource({"dummy,jeap-error-handling-service,/api/tasks/{param}",
            "shared-agir-service,dummy,/api/tasks/{param}",
            "jeap-mobile-app,jeap-error-handling-service,/api/tasks/{param}"})
    void importIntoModel_invalidData_relationNotImportedInModel(String provider, String consumer, String datapoint) {

        //given
        List<JeapRelation> jeapRelations = List.of(
                new JeapRelation(provider, consumer, "PUT", "http", datapoint));
        when(grafanaClient.apiRelations()).thenReturn(jeapRelations);

        //when
        importer.importIntoModel(model);

        //then
        assertTrue(shared.getRestApis().isEmpty());
        assertTrue(shared.getRelations().isEmpty());
    }

    @BeforeEach
    void buildModel() {
        ch.admin.bit.jeap.archrepo.metamodel.System jeap = ch.admin.bit.jeap.archrepo.metamodel.System.builder()
                .name("jeap")
                .systemComponents(List.of(
                        BackendService.builder()
                                .name("jeap-error-handling-service")
                                .build(),
                        MobileApp.builder()
                                .name("jeap-mobile-app")
                                .build()
                ))
                .build();

        shared = ch.admin.bit.jeap.archrepo.metamodel.System.builder()
                .name("agir")
                .systemComponents(List.of(BackendService.builder()
                        .name("shared-agir-service")
                        .build()))
                .build();

        model = ArchitectureModel.builder()
                .systems(List.of(jeap, shared))
                .build();
    }
}