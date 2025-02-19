package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos;

import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.NamespaceStagePair;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.RhosGrafanaClient;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SelfContainedSystem;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class RhosSystemComponentImporterTest {

    public static final String PREFIX = "EZV";
    public static final String SYSTEM = "jme";
    public static final String NEW_APP = "existingsystem-newapp-service";
    public static final String EXISTING_APP = "existingsystem-app1-service";
    public static final String DEPRECATED_APP = "existingsystem-deprecated-service";
    public static final String NOT_YET_DEPRECATED_APP = "existingsystem-notyetdeprecated-service";
    private static final String STAGE = "r";
    @Mock
    private RhosGrafanaClient grafanaClient;
    private ArchitectureModel model;
    private System system;
    private RhosSystemComponentImporter systemComponentImporter;

    @Test
    void importIntoModel() {
        NamespaceStagePair namespaceStagePair = new NamespaceStagePair("bit-jme-r", "r");
        doReturn(Set.of(namespaceStagePair)).when(grafanaClient).namespaces(Set.of(STAGE));
        doReturn(Set.of(NEW_APP, EXISTING_APP)).when(grafanaClient).services(namespaceStagePair);

        systemComponentImporter.importIntoModel(model);

        assertThat(system.getSystemComponents().stream().filter(sc -> sc.getImporter().equals(Importer.GRAFANA)).map(SystemComponent::getName).toList())
                .hasSize(3)
                .contains(EXISTING_APP, NOT_YET_DEPRECATED_APP, NEW_APP);
    }

    @BeforeEach
    void setUp() {
        SelfContainedSystem existingAppScs = SelfContainedSystem.builder()
                .name(EXISTING_APP)
                .importer(Importer.GRAFANA)
                .build();
        SelfContainedSystem deprecatedScs = SelfContainedSystem.builder()
                .name(DEPRECATED_APP)
                .importer(Importer.GRAFANA)
                .build();

        deprecatedScs.setLastSeenFromDate(ZonedDateTime.now().minusDays(25));

        SelfContainedSystem notYetDeprecatedScs = SelfContainedSystem.builder()
                .name(NOT_YET_DEPRECATED_APP)
                .importer(Importer.GRAFANA)
                .build();

        notYetDeprecatedScs.setLastSeenFromNow();

        system = System.builder()
                .name(SYSTEM)
                .systemComponents(List.of(existingAppScs, deprecatedScs, notYetDeprecatedScs))
                .build();
        model = ArchitectureModel.builder()
                .systems(List.of(system))
                .build();

        RhosSystemComponentImporterProperties props = new RhosSystemComponentImporterProperties();
        props.setImportedStages(Set.of(STAGE));

        systemComponentImporter = new RhosSystemComponentImporter(props, grafanaClient, new RhosSystemNameExtractor());
    }
}