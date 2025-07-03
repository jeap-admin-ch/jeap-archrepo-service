package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class SystemComponentRepositoryTest {

    @Autowired
    private SystemComponentRepository systemComponentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private SystemRepository systemRepository;

    @Test
    void saveSystemComponent_withValidSystem_persistsSuccessfully() {
        Team team = teamRepository.save(Team.builder().name("team").build());
        System system = System.builder().name("ValidSystem").defaultOwner(team).build();
        SystemComponent systemComponent = BackendService.builder()
                .id(UUID.randomUUID())
                .name("ValidSystemComponent")
                .ownedBy(team)
                .build();
        ReflectionTestUtils.setField(systemComponent, "createdAt", ZonedDateTime.now());
        system.addSystemComponent(systemComponent);
        systemRepository.save(system);

        Optional<SystemComponent> optionalSystemComponent = systemComponentRepository.findByNameContainingIgnoreCase("ValidSystemComponent");

        assertThat(optionalSystemComponent)
                .isPresent()
                .get()
                .extracting(SystemComponent::getId)
                .isEqualTo(systemComponent.getId());
    }

    @Test
    void saveSystemComponent_withoutSystem_throwsDataIntegrityViolationException() {
        Team team = teamRepository.save(Team.builder().name("team").build());

        SystemComponent systemComponent = BackendService.builder()
                .id(UUID.randomUUID())
                .name("orphaned-service")
                .ownedBy(team)
                .build();
        ReflectionTestUtils.setField(systemComponent, "createdAt", ZonedDateTime.now());

        assertThatThrownBy(() -> saveAndFlush(systemComponent))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findSystemComponent_byNonExistingName_returnsEmptyOptional() {
        Optional<SystemComponent> optionalSystemComponent = systemComponentRepository.findByNameContainingIgnoreCase("nonExistingName");

        assertThat(optionalSystemComponent).isNotPresent();
    }

    private void saveAndFlush(SystemComponent systemComponent) {
        systemComponentRepository.save(systemComponent);
        systemComponentRepository.flush();
    }
}
