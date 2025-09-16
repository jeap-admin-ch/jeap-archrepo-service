package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemGraph;
import ch.admin.bit.jeap.archrepo.persistence.SystemGraphRepository;
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

@SpringBootTest(classes = {
        ReactionsObserverImporterConfiguration.class,
        TestConfig.class
}, properties = {
        "reactionobserverservice.url=http://localhost:${wiremock.server.port}/reaction-observer-service",
        "reactionobserverservice.username=user",
        "reactionobserverservice.password=secret",
        "spring.application.name=test"})
@AutoConfigureWireMock(port = 0)
class SystemGraphImporterWireMockTest {

    @Autowired
    private SystemGraphImporter importer;

    @Autowired
    private SystemGraphRepository systemGraphRepository;

    @BeforeEach
    void setUp() {
        reset(systemGraphRepository);
        when(systemGraphRepository.save(any(SystemGraph.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(systemGraphRepository.existsBySystemName(any())).thenReturn(false);
    }

    @Test
    void importIntoModel_withSystemGraphs_savesSystemGraphs() {
        // Arrange
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());

        stubFor(get(urlEqualTo("/reaction-observer-service/api/systems/names"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                ["system1", "system2"]""")));

        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/systems/system1"))
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
                                           "component": "test-component1"
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
                                  "fingerprint": "fingerprint-system1"
                                }""")));

        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/systems/system2"))
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
                                           "component": "test-component2"
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
                                  "fingerprint": "fingerprint-system2"
                                }""")));

        // Stub for system3 graph (404 response - system has no graph, it will be skipped)
        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/systems/system3"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withBody("")));

        ArchitectureModel testModel = ArchitectureModel.builder().build();

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        verify(systemGraphRepository, times(2)).existsBySystemName(any());

        ArgumentCaptor<SystemGraph> systemGraphCaptor = ArgumentCaptor.forClass(SystemGraph.class);
        verify(systemGraphRepository, times(2)).save(systemGraphCaptor.capture());

        List<SystemGraph> savedGraphs = systemGraphCaptor.getAllValues();

        // Verify system1 graph
        SystemGraph system1Graph = savedGraphs.stream()
                .filter(g -> "system1".equals(g.getSystemName()))
                .findFirst()
                .orElseThrow();
        assertThat(system1Graph.getSystemName()).isEqualTo("system1");
        assertThat(system1Graph.getFingerprint()).isEqualTo("fingerprint-system1");
        assertThat(system1Graph.getGraphData()).isNotNull();

        // Verify the serialized graph data contains expected content
        String system1GraphJson = new String(system1Graph.getGraphData());
        assertThat(system1GraphJson)
                .contains("test-component1")
                .contains("MyEvent1");

        // Verify system2 graph
        SystemGraph system2Graph = savedGraphs.stream()
                .filter(g -> "system2".equals(g.getSystemName()))
                .findFirst()
                .orElseThrow();
        assertThat(system2Graph.getSystemName()).isEqualTo("system2");
        assertThat(system2Graph.getFingerprint()).isEqualTo("fingerprint-system2");
        assertThat(system2Graph.getGraphData()).isNotNull();

        // Verify the serialized graph data contains expected content
        String system2GraphJson = new String(system2Graph.getGraphData());
        assertThat(system2GraphJson)
                .contains("test-component2")
                .contains("MyEvent2");
    }

    @Test
    void importIntoModel_withSystemGraphEndpointReturning404_skipsSave() {
        // Arrange
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());

        // Stub for system names endpoint
        stubFor(get(urlEqualTo("/reaction-observer-service/api/systems/names"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                ["system-no-graph"]""")));

        // Stub for system graph returning 404
        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/systems/system-no-graph"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.NOT_FOUND.value())
                        .withBody("")));

        ArchitectureModel testModel = ArchitectureModel.builder().build();

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        verify(systemGraphRepository, never()).existsBySystemName(any());
        verify(systemGraphRepository, never()).save(any());
        verify(systemGraphRepository, never()).updateGraphDataAndFingerprintIfFingerprintChanged(any(), any(), any());
    }

    @Test
    void importIntoModel_withExistingSystemGraph_updatesSystemGraph() {
        // Arrange
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());

        // Stub for system names endpoint
        stubFor(get(urlEqualTo("/reaction-observer-service/api/systems/names"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                ["existing-system"]
                                """)));

        // Stub for system graph
        stubFor(get(urlEqualTo("/reaction-observer-service/api/graphs/systems/existing-system"))
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

        // Mock existing system
        when(systemGraphRepository.existsBySystemName("existing-system")).thenReturn(true);

        ArchitectureModel testModel = ArchitectureModel.builder().build();

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        verify(systemGraphRepository).existsBySystemName("existing-system");
        verify(systemGraphRepository).updateGraphDataAndFingerprintIfFingerprintChanged(eq("existing-system"), any(byte[].class), eq("updated-fingerprint"));
        verify(systemGraphRepository, never()).save(any());
    }

}
