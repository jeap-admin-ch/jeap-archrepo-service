package ch.admin.bit.jeap.archrepo.importer.prometheus.cf;

import ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client.CloudFoundryPrometheusClient;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class CloudFoundrySystemComponentImporterTest {

    public static final String PREFIX = "EZV";
    public static final String SYSTEM = "ExistingSystem";
    public static final String EXISTING_ORG = "existing-org";
    public static final String NEW_APP = "existingsystem-newapp-service";
    public static final String EXISTING_APP = "existingsystem-app1-service";
    public static final String DEPRECATED_APP = "existingsystem-deprecated-service";
    public static final String VENERABLE_APP = "existingsystem-app-service-venerable";
    public static final String NOT_YET_DEPRECATED_APP = "existingsystem-notyetdeprecated-service";
    private static final String SPACE = "ref";
    @Mock
    private CloudFoundryPrometheusClient cloudFoundryPrometheusClient;
    private ArchitectureModel model;
    private System system;
    private CloudFoundrySystemComponentImporter cloudFoundrySystemComponentImporter;

    @Test
    void importIntoModel() {
        doReturn(Set.of(EXISTING_ORG)).when(cloudFoundryPrometheusClient).listOrganisationWithMatchingPrefix(any(), anyString());
        doReturn(Set.of(EXISTING_APP, NEW_APP, VENERABLE_APP)).when(cloudFoundryPrometheusClient).listApps(EXISTING_ORG, SPACE);
        assertThat(system.getSystemComponents().stream().filter(sc -> sc.getImporter().equals(Importer.GRAFANA)).map(SystemComponent::getName).toList())
                .hasSize(3)
                .contains(EXISTING_APP, DEPRECATED_APP, NOT_YET_DEPRECATED_APP);

        cloudFoundrySystemComponentImporter.importIntoModel(model, "ref");

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

        CloudFoundrySystemComponentImporterProperties props = new CloudFoundrySystemComponentImporterProperties();
        props.setOrgPrefixes(List.of(PREFIX));

        cloudFoundrySystemComponentImporter = new CloudFoundrySystemComponentImporter(props, cloudFoundryPrometheusClient);
    }
}
