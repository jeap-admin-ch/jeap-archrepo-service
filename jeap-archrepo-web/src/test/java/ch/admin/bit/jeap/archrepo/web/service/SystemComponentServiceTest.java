package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.SystemComponentRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemComponentServiceTest {

    @Mock
    private SystemRepository systemRepository;
    @Mock
    private SystemComponentRepository systemComponentRepository;

    private SystemComponentService systemComponentService;

    @BeforeEach
    void setUp() {
        systemComponentService = new SystemComponentService(systemRepository, systemComponentRepository);
    }

    @Test
    void findOrCreateSystemComponentReturnsExistingComponent() {
        String componentName = "TestComponent";
        SystemComponent existingComponent = mock(SystemComponent.class);
        when(systemComponentRepository.findByNameIgnoreCase(componentName)).thenReturn(Optional.of(existingComponent));

        SystemComponent result = systemComponentService.findOrCreateSystemComponent(componentName);

        assertThat(result).isEqualTo(existingComponent);
        verify(systemComponentRepository).findByNameIgnoreCase(componentName);
        verifyNoInteractions(systemRepository);
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
    }

    @Test
    void unknownSystemDoesNotCreateAnything() {
        String componentName = "NewSystem-NewComponent";
        String systemName = "NewSystem";
        when(systemComponentRepository.findByNameIgnoreCase(componentName)).thenReturn(Optional.empty());
        when(systemRepository.findByNameIgnoreCase(systemName)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> systemComponentService.findOrCreateSystemComponent(componentName))
                .isInstanceOf(UnknownSystemException.class).hasMessageContaining(componentName, systemName);
        verify(systemRepository, never()).save(any());
        verify(systemComponentRepository, never()).save(any());
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
