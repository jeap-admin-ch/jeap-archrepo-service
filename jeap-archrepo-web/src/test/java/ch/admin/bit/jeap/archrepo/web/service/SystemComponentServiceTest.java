package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.persistence.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemComponentServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private SystemRepository systemRepository;
    @Mock
    private SystemComponentRepository systemComponentRepository;

    private SystemComponentService systemComponentService;

    @BeforeEach
    void setUp() {
        systemComponentService = new SystemComponentService(teamRepository, systemRepository, systemComponentRepository);
    }

    @Test
    void findOrCreateSystemComponentReturnsExistingComponent() {
        String componentName = "TestComponent";
        SystemComponent existingComponent = mock(SystemComponent.class);
        when(systemComponentRepository.findByNameIgnoreCase(componentName)).thenReturn(Optional.of(existingComponent));

        SystemComponent result = systemComponentService.findOrCreateSystemComponent(componentName);

        assertThat(result).isEqualTo(existingComponent);
        verify(systemComponentRepository).findByNameIgnoreCase(componentName);
        verifyNoInteractions(systemRepository, teamRepository);
    }

    @Test
    void findOrCreateSystemComponentCreatesNewComponentWhenSystemExists() {
        String componentName = "TestSystem-TestComponent";
        String systemName = "TestSystem";
        System existingSystem = createSystem(systemName);
        when(systemComponentRepository.findByNameIgnoreCase(componentName)).thenReturn(Optional.empty());
        when(systemRepository.findByNameIgnoreCase(systemName)).thenReturn(Optional.of(existingSystem));

        SystemComponent result = systemComponentService.findOrCreateSystemComponent(componentName);

        assertThat(result.getName()).isEqualTo(componentName);
        assertThat(result.getParent()).isEqualTo(existingSystem);
        verify(systemRepository).findByNameIgnoreCase(systemName);
        verify(systemComponentRepository).findByNameIgnoreCase(componentName);
        verify(systemRepository, never()).save(any());
        verify(teamRepository, never()).save(any());
    }

    @Test
    void findOrCreateSystemComponentCreatesNewComponentWhenSystemWithAliasExists() {
        String componentName = "TestAliasSystem-TestComponent";
        String systemName = "TestSystem";
        System existingSystem = createSystemWithAlias(systemName, "TestAliasSystem");
        when(systemComponentRepository.findByNameIgnoreCase(componentName)).thenReturn(Optional.empty());
        when(systemRepository.findAll()).thenReturn(List.of(existingSystem));

        SystemComponent result = systemComponentService.findOrCreateSystemComponent(componentName);

        assertThat(result.getName()).isEqualTo(componentName);
        assertThat(result.getParent()).isEqualTo(existingSystem);
        verify(systemRepository).findByNameIgnoreCase("TestAliasSystem");
        verify(systemComponentRepository).findByNameIgnoreCase(componentName);
        verify(systemRepository, never()).save(any());
        verify(teamRepository, never()).save(any());
    }

    @Test
    void findOrCreateSystemComponentCreatesNewComponentAndSystem() {
        String componentName = "NewSystem-NewComponent";
        String systemName = "NewSystem";
        Team newTeam = Team.builder().name(systemName).build();
        System newSystem = createSystem(systemName);
        when(systemComponentRepository.findByNameIgnoreCase(componentName)).thenReturn(Optional.empty());
        when(systemRepository.findByNameIgnoreCase(systemName)).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenReturn(newTeam);
        when(systemRepository.save(any(System.class))).thenReturn(newSystem);

        SystemComponent result = systemComponentService.findOrCreateSystemComponent(componentName);

        assertThat(result.getName()).isEqualTo(componentName);
        assertThat(result.getParent()).isEqualTo(newSystem);
        verify(teamRepository).save(any(Team.class));
        verify(systemRepository).save(any(System.class));
        verify(systemComponentRepository).findByNameIgnoreCase(componentName);
    }

    private System createSystem(String systemName) {
        return System.builder()
                .name(systemName)
                .defaultOwner(Team.builder().name(systemName).build())
                .build();
    }

    private System createSystemWithAlias(String systemName, String alias) {
        return System.builder()
                .name(systemName)
                .defaultOwner(Team.builder().name(systemName).build())
                .aliases(List.of(alias))
                .build();
    }

}
