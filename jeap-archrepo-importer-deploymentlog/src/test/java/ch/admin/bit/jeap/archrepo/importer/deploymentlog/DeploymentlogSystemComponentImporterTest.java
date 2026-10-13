package ch.admin.bit.jeap.archrepo.importer.deploymentlog;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DeploymentlogImporterConfiguration.class, properties = {
        "deploymentlog.username=user",
        "deploymentlog.password=secret",
        "spring.application.name=test"})
class DeploymentlogSystemComponentImporterTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .configureStaticDsl(true)
            .build();

    @DynamicPropertySource
    static void registerWireMockProperties(DynamicPropertyRegistry registry) {
        registry.add("deploymentlog.url", () -> wireMock.baseUrl() + "/deploymentlog-service");
    }

    @Autowired
    private DeploymentlogSystemComponentImporter importer;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
    }

    @Test
    void importIntoModel() {
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());
        stubFor(get(urlEqualTo("/deploymentlog-service/api/environment/ref/components?includeUndeployed=true"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                {"componentName":"system-existing-component", "version":"1.0.0"},
                                {"componentName":"system-new-component", "version":"2.0.0"},
                                {"componentName":"system-undeployed-component", "version":"1.0.0", "undeployed":true, "undeployedAt":"2026-10-01T00:00:00Z"}
                                ]""")));
        ArchitectureModel testModel = createTestModel();

        importer.importIntoModel(testModel, "ref");

        System system = testModel.findSystem("SYSTEM").orElseThrow();
        Set<String> componentNames = system.getSystemComponentsIncludingUndeployed().stream().map(SystemComponent::getName).collect(toSet());
        assertThat(componentNames)
                .describedAs("Removed component is deleted, undeployed component is kept, components from other importers are left as-is")
                .containsOnly(
                        "system-existing-component",
                        "system-new-component",
                        "system-undeployed-component",
                        "system-component-dont-touch");
        assertThat(testModel.findSystemComponent("system-existing-component").orElseThrow())
                .describedAs("Existing component from other importer is not touched")
                .matches(systemComponent -> systemComponent.getImporter() == Importer.GRAFANA);
        assertThat(testModel.findSystemComponent("system-new-component").orElseThrow())
                .describedAs("New component is imported")
                .matches(systemComponent -> systemComponent.getImporter() == Importer.DEPLOYMENT_LOG);
        assertThat(testModel.findSystemComponent("system-undeployed-component").orElseThrow())
                .describedAs("Undeployed component is kept, but marked undeployed instead of being deleted")
                .matches(SystemComponent::isUndeployed)
                .matches(systemComponent -> systemComponent.getUndeployedAt().toInstant().equals(java.time.Instant.parse("2026-10-01T00:00:00Z")));
    }

    @Test
    void importIntoModel_whenGrafanaComponentIsReportedUndeployed_thenMarkedUndeployedAndStaysGrafana() {
        stubFor(get(urlEqualTo("/deploymentlog-service/api/environment/ref/components?includeUndeployed=true"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                {"componentName":"system-existing-component", "version":"1.0.0", "undeployed":true, "undeployedAt":"2026-10-01T00:00:00Z"}
                                ]""")));
        ArchitectureModel testModel = createTestModel();

        importer.importIntoModel(testModel, "ref");

        SystemComponent component = testModel.findSystemComponent("system-existing-component").orElseThrow();
        assertThat(component.isUndeployed()).isTrue();
        assertThat(component.getImporter()).isEqualTo(Importer.GRAFANA);
    }

    @Test
    void importIntoModel_whenUndeployedComponentIsRedeployed_thenMarkedDeployedAgain() {
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());
        stubFor(get(urlEqualTo("/deploymentlog-service/api/environment/ref/components?includeUndeployed=true"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                {"componentName":"system-deleted-component", "version":"1.0.0"}
                                ]""")));
        ArchitectureModel testModel = createTestModel();
        testModel.findSystemComponent("system-deleted-component").orElseThrow()
                .markUndeployed(java.time.ZonedDateTime.now());

        importer.importIntoModel(testModel, "ref");

        assertThat(testModel.findSystemComponent("system-deleted-component").orElseThrow())
                .describedAs("Redeployed component is marked deployed again")
                .matches(systemComponent -> !systemComponent.isUndeployed())
                .matches(systemComponent -> systemComponent.getUndeployedAt() == null);
    }

    private ArchitectureModel createTestModel() {
        SystemComponent component1 = BackendService.builder()
                .name("system-existing-component")
                .importer(Importer.GRAFANA)
                .build();
        SystemComponent component2 = BackendService.builder()
                .name("system-deleted-component")
                .importer(Importer.DEPLOYMENT_LOG)
                .build();
        SystemComponent component3 = BackendService.builder()
                .name("system-component-dont-touch")
                .importer(Importer.GRAFANA)
                .build();
        SystemComponent component4 = BackendService.builder()
                .name("system-undeployed-component")
                .importer(Importer.DEPLOYMENT_LOG)
                .build();
        System system = System.builder()
                .name("SYSTEM")
                .systemComponents(List.of(component1, component2, component3, component4))
                .build();
        return ArchitectureModel.builder()
                .systems(List.of(system))
                .build();
    }
}
