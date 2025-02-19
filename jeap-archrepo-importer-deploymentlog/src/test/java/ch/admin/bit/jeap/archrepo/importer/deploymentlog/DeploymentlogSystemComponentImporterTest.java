package ch.admin.bit.jeap.archrepo.importer.deploymentlog;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DeploymentlogImporterConfiguration.class, properties = {
        "deploymentlog.url=http://localhost:${wiremock.server.port}/deploymentlog-service",
        "deploymentlog.username=user",
        "deploymentlog.password=secret",
        "spring.application.name=test"})
@AutoConfigureWireMock(port = 0)
class DeploymentlogSystemComponentImporterTest {

    @Autowired
    private DeploymentlogSystemComponentImporter importer;

    @Test
    void importIntoModel() {
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());
        stubFor(get(urlEqualTo("/deploymentlog-service/api/environment/ref/components"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                {"componentName":"system-existing-component", "version":"1.0.0"},
                                {"componentName":"system-new-component", "version":"2.0.0"}
                                ]""")));
        ArchitectureModel testModel = createTestModel();

        importer.importIntoModel(testModel);

        System system = testModel.findSystem("SYSTEM").orElseThrow();
        Set<String> componentNames = system.getSystemComponents().stream().map(SystemComponent::getName).collect(toSet());
        assertThat(componentNames)
                .describedAs("Removed component is deleted, components from other importers are left as-is")
                .containsOnly(
                        "system-existing-component",
                        "system-new-component",
                        "system-component-dont-touch");
        assertThat(testModel.findSystemComponent("system-existing-component").orElseThrow())
                .describedAs("Existing component from other importer is not touched")
                .matches(systemComponent -> systemComponent.getImporter() == Importer.GRAFANA);
        assertThat(testModel.findSystemComponent("system-new-component").orElseThrow())
                .describedAs("New component is imported")
                .matches(systemComponent -> systemComponent.getImporter() == Importer.DEPLOYMENT_LOG);
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
        System system = System.builder()
                .name("SYSTEM")
                .systemComponents(List.of(component1, component2, component3))
                .build();
        return ArchitectureModel.builder()
                .systems(List.of(system))
                .build();
    }
}
