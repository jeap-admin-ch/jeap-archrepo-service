package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos;

import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.RhosGrafanaClient;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SelfContainedSystem;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
    @Mock
    private RhosGrafanaClient grafanaClient;
    private ArchitectureModel model;
    private System system;
    private RhosSystemComponentImporter systemComponentImporter;

    @Test
    void importIntoModel() {
        doReturn(Set.of("bit-jme-r")).when(grafanaClient).namespaces("ref");
        doReturn(Set.of(NEW_APP, EXISTING_APP)).when(grafanaClient).services("ref", "bit-jme-r");

        systemComponentImporter.importIntoModel(model, "ref");

        assertThat(system.getSystemComponentsIncludingUndeployed().stream().filter(sc -> sc.getImporter().equals(Importer.GRAFANA)).map(SystemComponent::getName).toList())
                .hasSize(3)
                .contains(EXISTING_APP, NOT_YET_DEPRECATED_APP, NEW_APP);
    }

    @Test
    void importIntoModel_keepsAComponentUndeployedWhileMetricsStillReportIt() {
        doReturn(Set.of("bit-jme-r")).when(grafanaClient).namespaces("ref");
        doReturn(Set.of(EXISTING_APP)).when(grafanaClient).services("ref", "bit-jme-r");
        ZonedDateTime undeployedAt = ZonedDateTime.now().minusDays(1);
        system.findSystemComponent(EXISTING_APP).orElseThrow().markUndeployed(undeployedAt);

        systemComponentImporter.importIntoModel(model, "ref");

        SystemComponent component = system.findSystemComponent(EXISTING_APP).orElseThrow();
        assertThat(component.isUndeployed()).isTrue();
        assertThat(component.getUndeployedAt()).isEqualTo(undeployedAt);
        assertThat(component.getImporter()).isEqualTo(Importer.GRAFANA);
    }

    @Test
    void importIntoModel_relocatesComponentFoundUnderADifferentSystemInsteadOfRecreatingIt() {
        String movedApp = "shared-moved-service";
        SystemComponent movedComponent = BackendService.builder()
                .name(movedApp)
                .importer(Importer.DEPLOYMENT_LOG)
                .build();
        System previousOwner = System.builder().name("previousOwner")
                .systemComponents(List.of(movedComponent))
                .build();
        System newOwner = System.builder().name("newOwner").aliases(List.of("shared")).build();
        ArchitectureModel testModel = ArchitectureModel.builder().systems(List.of(previousOwner, newOwner)).build();
        doReturn(Set.of("bit-shared-r")).when(grafanaClient).namespaces("ref");
        doReturn(Set.of(movedApp)).when(grafanaClient).services("ref", "bit-shared-r");

        systemComponentImporter.importIntoModel(testModel, "ref");

        assertThat(newOwner.findSystemComponent(movedApp))
                .describedAs("Component moves to the system its prefix now resolves to, instead of being recreated there")
                .contains(movedComponent);
        assertThat(previousOwner.getSystemComponentsIncludingUndeployed())
                .describedAs("Component no longer lives under its previous owner")
                .isEmpty();
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void importIntoModel_logsAWarningForANamespaceOfAnUnknownSystem(CapturedOutput output) {
        doReturn(Set.of("bit-undeclared-r")).when(grafanaClient).namespaces("ref");

        systemComponentImporter.importIntoModel(model, "ref");

        assertThat(output.getAll().lines().filter(line -> line.contains("systems not declared in archrepo.model")))
                .singleElement()
                .satisfies(line -> assertThat(line).contains("WARN").contains("undeclared"));
        assertThat(output.getAll().lines().filter(line -> line.contains("undeclared")))
                .noneMatch(line -> line.contains("ERROR"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void importIntoModel_keepsAComponentReportedInTheNamespacesOfTwoSystemsWhereItIsAndLogsAnError(CapturedOutput output) {
        // The system comes from the namespace here, not from the component name. Moving the component to whichever
        // namespace reported it last would move it back and forth on every run.
        System otherSystem = System.builder().name("abc").build();
        ArchitectureModel twoSystems = ArchitectureModel.builder().systems(List.of(system, otherSystem)).build();
        SystemComponent component = system.findSystemComponent(EXISTING_APP).orElseThrow();
        doReturn(Set.of("bit-jme-r", "bit-abc-r")).when(grafanaClient).namespaces("ref");
        doReturn(Set.of(EXISTING_APP)).when(grafanaClient).services("ref", "bit-jme-r");
        doReturn(Set.of(EXISTING_APP)).when(grafanaClient).services("ref", "bit-abc-r");

        systemComponentImporter.importIntoModel(twoSystems, "ref");

        assertThat(component.getParent()).isSameAs(system);
        assertThat(otherSystem.getSystemComponentsIncludingUndeployed()).isEmpty();
        assertThat(output.getAll().lines().filter(line -> line.contains(EXISTING_APP)))
                .anyMatch(line -> line.contains("ERROR") && line.contains("abc"));
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

        systemComponentImporter = new RhosSystemComponentImporter(grafanaClient, new RhosSystemNameExtractor());
    }
}
