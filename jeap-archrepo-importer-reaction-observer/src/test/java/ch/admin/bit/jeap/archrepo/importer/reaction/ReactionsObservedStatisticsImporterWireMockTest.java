package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.reaction.ReactionStatistics;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.ReactionStatisticsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        ReactionsObserverImporterConfiguration.class,
        ReactionsObservedStatisticsImporterWireMockTest.TestConfig.class
}, properties = {
        "reactionobserverservice.url=http://localhost:${wiremock.server.port}/reaction-observer-service",
        "reactionobserverservice.username=user",
        "reactionobserverservice.password=secret",
        "spring.application.name=test"})
@AutoConfigureWireMock(port = 0)
class ReactionsObservedStatisticsImporterWireMockTest {

    @Autowired
    private ReactionsObservedStatisticsImporter importer;

    @Test
    void importIntoModel() {
        // Arrange
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());

        // Stub for first component with statistics
        stubFor(get(urlEqualTo("/reaction-observer-service/api/statisticsV2/system-existing-component"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                {
                                  "component": "system-existing-component",
                                  "triggerType": "TestTriggerType",
                                  "triggerFqn": "com.example.TestTrigger",
                                  "actions": [
                                    {
                                      "actionType": "TestActionType",
                                      "actionFqn": "com.example.TestAction",
                                      "actionProperties": {}
                                    }
                                  ],
                                  "count": 100,
                                  "median": 50.0,
                                  "percentage": 75.0,
                                  "triggerProperties": {}
                                }
                                ]""")));

        // Stub for second component with statistics
        stubFor(get(urlEqualTo("/reaction-observer-service/api/statisticsV2/system-new-component"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                [
                                {
                                  "component": "system-new-component",
                                  "triggerType": "AnotherTriggerType",
                                  "triggerFqn": "com.example.AnotherTrigger",
                                  "actions": [
                                    {
                                      "actionType": "AnotherActionType",
                                      "actionFqn": "com.example.AnotherAction",
                                      "actionProperties": {}
                                    }
                                  ],
                                  "count": 200,
                                  "median": 60.0,
                                  "percentage": 85.0,
                                  "triggerProperties": {}
                                }
                                ]""")));

        // Stub for third component with no statistics
        stubFor(get(urlEqualTo("/reaction-observer-service/api/statisticsV2/system-component-no-statistics"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Basic " + basicAuth))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        ArchitectureModel testModel = createTestModel();

        // Act
        importer.importIntoModel(testModel, "ref");

        // Assert
        // Verify that the components have the correct statistics
        SystemComponent component1 = testModel.findSystemComponent("system-existing-component").orElseThrow();
        assertThat(component1.getReactionStatistics()).isNotEmpty();
        ReactionStatistics stats1 = component1.getReactionStatistics().getFirst();
        assertThat(stats1.getTriggerType()).isEqualTo("TestTriggerType");
        assertThat(stats1.getTriggerFqn()).isEqualTo("com.example.TestTrigger");
        assertThat(stats1.getActions().getFirst().getActionType()).isEqualTo("TestActionType");
        assertThat(stats1.getActions().getFirst().getActionFqn()).isEqualTo("com.example.TestAction");
        assertThat(stats1.getCount()).isEqualTo(100);
        assertThat(stats1.getMedian()).isEqualTo(50.0);
        assertThat(stats1.getPercentage()).isEqualTo(75.0);

        // Check that actions are created and associated with the ReactionStatistics entity
        assertThat(stats1.getActions()).hasSize(1);
        assertThat(stats1.getActions().get(0).getActionType()).isEqualTo("TestActionType");
        assertThat(stats1.getActions().get(0).getActionFqn()).isEqualTo("com.example.TestAction");
        assertThat(stats1.getActions().get(0).getReactionStatistics()).isEqualTo(stats1);

        SystemComponent component2 = testModel.findSystemComponent("system-new-component").orElseThrow();
        assertThat(component2.getReactionStatistics()).isNotEmpty();
        ReactionStatistics stats2 = component2.getReactionStatistics().getFirst();
        assertThat(stats2.getTriggerType()).isEqualTo("AnotherTriggerType");
        assertThat(stats2.getTriggerFqn()).isEqualTo("com.example.AnotherTrigger");
        assertThat(stats2.getActions().getFirst().getActionType()).isEqualTo("AnotherActionType");
        assertThat(stats2.getActions().getFirst().getActionFqn()).isEqualTo("com.example.AnotherAction");
        assertThat(stats2.getCount()).isEqualTo(200);
        assertThat(stats2.getMedian()).isEqualTo(60.0);
        assertThat(stats2.getPercentage()).isEqualTo(85.0);

        // Check that actions are created and associated with the ReactionStatistics entity
        assertThat(stats2.getActions()).hasSize(1);
        assertThat(stats2.getActions().get(0).getActionType()).isEqualTo("AnotherActionType");
        assertThat(stats2.getActions().get(0).getActionFqn()).isEqualTo("com.example.AnotherAction");
        assertThat(stats2.getActions().get(0).getReactionStatistics()).isEqualTo(stats2);

        // Verify that the third component doesn't have statistics
        SystemComponent component3 = testModel.findSystemComponent("system-component-no-statistics").orElseThrow();
        assertThat(component3.getReactionStatistics()).isEmpty();
    }

    private ArchitectureModel createTestModel() {
        SystemComponent component1 = BackendService.builder()
                .name("system-existing-component")
                .importer(Importer.GRAFANA)
                .build();
        SystemComponent component2 = BackendService.builder()
                .name("system-new-component")
                .importer(Importer.GRAFANA)
                .build();
        SystemComponent component3 = BackendService.builder()
                .name("system-component-no-statistics")
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

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ReactionStatisticsRepository reactionStatisticsRepository() {
            ReactionStatisticsRepository mockRepository = mock(ReactionStatisticsRepository.class);
            when(mockRepository.save(any(ReactionStatistics.class))).thenAnswer(invocation -> invocation.getArgument(0));
            return mockRepository;
        }
    }
}
