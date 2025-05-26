package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionsObservedStatisticsDto;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.reaction.ReactionStatistics;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.ReactionStatisticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionsObservedStatisticsImporterTest {

    @Mock
    private ReactionObserverService reactionObserverService;

    @Mock
    private ReactionStatisticsRepository reactionStatisticsRepository;

    @Mock
    private ArchitectureModel model;

    @Mock
    private System system;

    @Captor
    private ArgumentCaptor<ReactionStatistics> reactionStatisticsCaptor;

    @Test
    void importIntoModel_withStatistics_savesReactionStatistics() {
        // Arrange
        String componentName = "testComponent";
        String systemName = "testSystem";
        when(model.getAllSystemComponentNamesWithSystemName()).thenReturn(singletonMap(componentName, systemName));
        
        ReactionsObservedStatisticsDto statisticsDto = createTestStatisticsDto(componentName);
        when(reactionObserverService.getReactionsObservedStatistics(componentName))
                .thenReturn(List.of(statisticsDto));
        
        SystemComponent systemComponent = BackendService.builder().name(componentName).build();
        when(model.findSystem(systemName)).thenReturn(Optional.of(system));
        when(system.findSystemComponent(componentName)).thenReturn(Optional.of(systemComponent));
        
        ReactionsObservedStatisticsImporter importer = new ReactionsObservedStatisticsImporter(
                reactionObserverService, reactionStatisticsRepository);
        
        // Act
        importer.importIntoModel(model);
        
        // Assert
        verify(reactionStatisticsRepository).deleteAll();
        verify(reactionStatisticsRepository).save(reactionStatisticsCaptor.capture());

        ReactionStatistics capturedStatistics = reactionStatisticsCaptor.getValue();
        assertThat(capturedStatistics.getComponent()).isEqualTo(systemComponent);
        assertThat(capturedStatistics.getTriggerType()).isEqualTo(statisticsDto.triggerType());
        assertThat(capturedStatistics.getTriggerFqn()).isEqualTo(statisticsDto.triggerFqn());
        assertThat(capturedStatistics.getActionType()).isEqualTo(statisticsDto.actionType());
        assertThat(capturedStatistics.getActionFqn()).isEqualTo(statisticsDto.actionFqn());
        assertThat(capturedStatistics.getCount()).isEqualTo((int) statisticsDto.count());
        assertThat(capturedStatistics.getMedian()).isEqualTo(statisticsDto.median());
        assertThat(capturedStatistics.getPercentage()).isEqualTo(statisticsDto.percentage());
    }

    @Test
    void importIntoModel_withoutStatistics_doesNotSaveAnything() {
        // Arrange
        String componentName = "testComponent";
        String systemName = "testSystem";
        
        when(model.getAllSystemComponentNamesWithSystemName()).thenReturn(singletonMap(componentName, systemName));
        
        when(reactionObserverService.getReactionsObservedStatistics(componentName))
                .thenReturn(Collections.emptyList());
        
        ReactionsObservedStatisticsImporter importer = new ReactionsObservedStatisticsImporter(
                reactionObserverService, reactionStatisticsRepository);
        
        // Act
        importer.importIntoModel(model);
        
        // Assert
        verify(reactionStatisticsRepository).deleteAll();
        verify(reactionStatisticsRepository, never()).save(any());
    }

    @Test
    void importIntoModel_withMultipleComponents_processesAllComponents() {
        // Arrange
        String component1 = "component1";
        String component2 = "component2";
        String systemName = "testSystem";
        
        Map<String, String> componentMap = new HashMap<>();
        componentMap.put(component1, systemName);
        componentMap.put(component2, systemName);
        when(model.getAllSystemComponentNamesWithSystemName()).thenReturn(componentMap);
        
        ReactionsObservedStatisticsDto statisticsDto1 = createTestStatisticsDto(component1);
        ReactionsObservedStatisticsDto statisticsDto2 = createTestStatisticsDto(component2);
        
        when(reactionObserverService.getReactionsObservedStatistics(component1))
                .thenReturn(List.of(statisticsDto1));
        when(reactionObserverService.getReactionsObservedStatistics(component2))
                .thenReturn(List.of(statisticsDto2));
        
        SystemComponent systemComponent1 = BackendService.builder().name(component1).build();
        SystemComponent systemComponent2 = BackendService.builder().name(component2).build();
        
        when(model.findSystem(systemName)).thenReturn(Optional.of(system));
        when(system.findSystemComponent(component1)).thenReturn(Optional.of(systemComponent1));
        when(system.findSystemComponent(component2)).thenReturn(Optional.of(systemComponent2));
        
        ReactionsObservedStatisticsImporter importer = new ReactionsObservedStatisticsImporter(
                reactionObserverService, reactionStatisticsRepository);
        
        // Act
        importer.importIntoModel(model);
        
        // Assert
        verify(reactionStatisticsRepository).deleteAll();
        verify(reactionStatisticsRepository, times(2)).save(any());
    }

    private ReactionsObservedStatisticsDto createTestStatisticsDto(String componentName) {
        return new ReactionsObservedStatisticsDto(
                componentName,
                "TestTriggerType",
                "com.example.TestTrigger",
                "TestActionType",
                "com.example.TestAction",
                100L,
                50.0,
                75.0,
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }
}