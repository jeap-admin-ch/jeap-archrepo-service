package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.ComponentGraph;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.ComponentGraphRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {
        ReactionsObserverImporterConfiguration.class,
        TestConfig.class
}, properties = {
        "reactionobserverservice.url=http://localhost:${wiremock.server.port}/reaction-observer-service",
        "reactionobserverservice.username=user",
        "reactionobserverservice.password=secret",
        "spring.application.name=test",
        "spring.main.allow-bean-definition-overriding=true"})
@AutoConfigureWireMock(port = 0)
class ComponentGraphImporterWireMockTest {

    @Autowired
    private ComponentGraphImporter importer;

    @Autowired
    private ComponentGraphRepository componentGraphRepository;

    @BeforeEach
    void setUp() {
        reset(componentGraphRepository);
        when(componentGraphRepository.save(any(ComponentGraph.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(componentGraphRepository.existsBySystemNameAndComponentName(any(), any())).thenReturn(false);
    }

    @Test
    void importIntoModel_withServiceGraphs_savesComponentGraphs() {
        // Arrange
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());

        stubFor(get(urlEqualTo("/reaction-observer-service/api/components/names"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [ "component1", "component2" ]
                                """)));

        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/components/component1"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "graph": {
                                    "nodes": [
                                         {
                                           "nodeType": "MESSAGE",
                                           "id": 123,
                                           "messageKey": "msg_123",
                                           "messageType": "MyEvent1",
                                           "variant": "default"
                                         },
                                         {
                                           "nodeType": "REACTION",
                                           "id": 77,
                                           "component": "component1"
                                         }
                                       ],
                                    "edges": [
                                       {
                                         "edgeType": "TRIGGER",
                                         "sourceId": 123,
                                         "sourceNodeType": "MESSAGE",
                                         "targetReactionId": 77,
                                         "median": 10
                                       },
                                       {
                                         "edgeType": "ACTION",
                                         "sourceReactionId": 77,
                                         "targetId": 123,
                                         "targetNodeType": "REACTION"
                                       }
                                     ]
                                  },
                                  "fingerprint": "fingerprint-component1"
                                }""")));

        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/components/component2"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "graph": {
                                    "nodes": [
                                         {
                                           "nodeType": "MESSAGE",
                                           "id": 1234,
                                           "messageKey": "msg_1234",
                                           "messageType": "MyEvent2",
                                           "variant": "default"
                                         },
                                         {
                                           "nodeType": "REACTION",
                                           "id": 778,
                                           "component": "component2"
                                         }
                                       ],
                                    "edges": [
                                       {
                                         "edgeType": "TRIGGER",
                                         "sourceId": 1234,
                                         "sourceNodeType": "MESSAGE",
                                         "targetReactionId": 778,
                                         "median": 10
                                       },
                                       {
                                         "edgeType": "ACTION",
                                         "sourceReactionId": 778,
                                         "targetId": 1234,
                                         "targetNodeType": "REACTION"
                                       }
                                     ]
                                  },
                                  "fingerprint": "fingerprint-component2"
                                }""")));

        // Create test model with components
        SystemComponent comp1 = BackendService.builder().name("component1").build();
        SystemComponent comp2 = BackendService.builder().name("component2").build();
        SystemComponent comp3 = BackendService.builder().name("component3-no-reactions").build();

        System system1 = System.builder().name("system1").systemComponents(List.of(comp1)).build();
        System system2 = System.builder().name("system2").systemComponents(List.of(comp2, comp3)).build();

        ArchitectureModel testModel = ArchitectureModel.builder()
                .systems(List.of(system1, system2))
                .build();

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        verify(componentGraphRepository, times(2)).existsBySystemNameAndComponentName(any(), any());

        ArgumentCaptor<ComponentGraph> componentGraphCaptor = ArgumentCaptor.forClass(ComponentGraph.class);
        verify(componentGraphRepository, times(2)).save(componentGraphCaptor.capture());

        List<ComponentGraph> savedGraphs = componentGraphCaptor.getAllValues();

        // Verify component1 graph
        ComponentGraph component1Graph = savedGraphs.stream()
                .filter(g -> "component1".equals(g.getComponentName()))
                .findFirst()
                .orElseThrow();
        assertThat(component1Graph.getSystemName()).isEqualTo("system1");
        assertThat(component1Graph.getComponentName()).isEqualTo("component1");
        assertThat(component1Graph.getFingerprint()).isEqualTo("fingerprint-component1");
        assertThat(component1Graph.getGraphData()).isNotNull();

        // Verify the serialized graph data contains expected content
        String component1GraphJson = new String(component1Graph.getGraphData());
        assertThat(component1GraphJson)
                .contains("component1")
                .contains("MyEvent1");

        // Verify component2 graph
        ComponentGraph component2Graph = savedGraphs.stream()
                .filter(g -> "component2".equals(g.getComponentName()))
                .findFirst()
                .orElseThrow();
        assertThat(component2Graph.getSystemName()).isEqualTo("system2");
        assertThat(component2Graph.getComponentName()).isEqualTo("component2");
        assertThat(component2Graph.getFingerprint()).isEqualTo("fingerprint-component2");
        assertThat(component2Graph.getGraphData()).isNotNull();

        // Verify the serialized graph data contains expected content
        String component2GraphJson = new String(component2Graph.getGraphData());
        assertThat(component2GraphJson)
                .contains("component2")
                .contains("MyEvent2");
    }

    @Test
    void importIntoModel_withServiceGraphEndpointReturning404_skipsSave() {
        // Arrange
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());

        stubFor(get(urlEqualTo("/reaction-observer-service/api/components/names"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [ "component1", "component2" ]
                                """)));

        // Stub for service graph returning 404
        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/components/component-no-graph"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withBody("")));

        // Create test model with component
        SystemComponent comp1 = BackendService.builder().name("component-no-graph").build();
        System system1 = System.builder().name("system1").systemComponents(List.of(comp1)).build();

        ArchitectureModel testModel = ArchitectureModel.builder()
                .systems(List.of(system1))
                .build();

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        verify(componentGraphRepository, never()).existsBySystemNameAndComponentName(any(), any());
        verify(componentGraphRepository, never()).save(any());
        verify(componentGraphRepository, never()).updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(any(), any(), any(), any());
    }

    @Test
    void importIntoModel_withExistingServiceGraph_updatesComponentGraph() {
        // Arrange
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());

        stubFor(get(urlEqualTo("/reaction-observer-service/api/components/names"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [ "existing-component" ]
                                """)));

        // Stub for service graph
        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/components/existing-component"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "graph": {
                                    "nodes": ["updated-node"],
                                    "edges": []
                                  },
                                  "fingerprint": "updated-fingerprint"
                                }""")));

        // Mock existing component
        when(componentGraphRepository.existsBySystemNameAndComponentName("existing-system", "existing-component")).thenReturn(true);

        // Create test model with component
        SystemComponent comp1 = BackendService.builder().name("existing-component").build();
        System system1 = System.builder().name("existing-system").systemComponents(List.of(comp1)).build();

        ArchitectureModel testModel = ArchitectureModel.builder()
                .systems(List.of(system1))
                .build();

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        verify(componentGraphRepository).existsBySystemNameAndComponentName("existing-system", "existing-component");
        verify(componentGraphRepository).updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(eq("existing-system"), eq("existing-component"), any(byte[].class), eq("updated-fingerprint"));
        verify(componentGraphRepository, never()).save(any());
    }

}
