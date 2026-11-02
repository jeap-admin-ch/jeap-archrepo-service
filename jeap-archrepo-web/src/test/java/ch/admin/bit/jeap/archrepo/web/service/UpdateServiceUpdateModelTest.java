package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.persistence.OpenApiSpecRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.persistence.TeamRepository;
import ch.admin.bit.jeap.archrepo.web.rest.model.ArchRepoTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import ch.admin.bit.jeap.archrepo.web.PostgresIntegrationTestBase;

@SpringBootTest(properties = {"archrepo-config.environment=dev"})
@Import(ArchRepoTestConfiguration.class)
@ActiveProfiles("test")
class UpdateServiceUpdateModelTest extends PostgresIntegrationTestBase {
    private static final String SYSTEM_NAME = "test";
    @Autowired
    UpdateService service;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @MockitoBean
    ArchRepoImporter importer;
    @MockitoBean
    TeamRepository teamRepository;
    @MockitoBean
    SystemRepository systemRepository;
    @MockitoBean
    OpenApiSpecRepository openApiSpecRepository;
    @MockitoBean
    ModelOnboardingService modelOnboardingService;

    /**
     * Every test starts unlocked: the scheduled update holds the lock for at least 5s, which would silently skip a
     * single-importer run right after it. The row is expired, not deleted - ShedLock remembers having created it
     * and from then on only updates it, so a deleted row would look locked for good.
     */
    @BeforeEach
    @AfterEach
    void releaseModelUpdateLock() {
        jdbcTemplate.update("update shedlock set lock_until = ? where name = ?",
                Timestamp.from(Instant.now().minus(Duration.ofDays(1))), UpdateService.MODEL_UPDATE_LOCK);
    }

    @Test
    void updateModel() {
        when(systemRepository.findAll()).thenReturn(List.of(System.builder().name(SYSTEM_NAME).build()));

        service.updateModel();

        var order = inOrder(modelOnboardingService, importer, systemRepository);
        order.verify(modelOnboardingService).apply(any(ArchitectureModel.class));
        order.verify(systemRepository).saveAll(any());
        order.verify(importer).importIntoModel(any(ArchitectureModel.class), eq("dev"));
        order.verify(systemRepository).saveAll(any());

        ArgumentCaptor<List<System>> systemCaptor = ArgumentCaptor.forClass(List.class);
        verify(systemRepository, times(2)).saveAll(systemCaptor.capture());

        assertThat(systemCaptor.getAllValues()).allSatisfy(saved -> {
            assertThat(saved).hasSize(1);
            assertThat(saved.getFirst().getName()).isEqualTo(SYSTEM_NAME);
        });
    }

    @Test
    void anImporterThatCreatesASystemFailsTheUpdate() {
        // Configuration is the master of systems and teams; the model is sealed once onboarding has applied it.
        doAnswer(invocation -> {
            invocation.<ArchitectureModel>getArgument(0).addSystem(System.builder().name("rogue").build());
            return null;
        }).when(importer).importIntoModel(any(ArchitectureModel.class), eq("dev"));

        String importerName = importer.getClass().getSimpleName();
        assertThatThrownBy(() -> service.runImporter(importerName))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("rogue");
        verify(systemRepository, times(1)).saveAll(any());
    }

    @Test
    void anImporterThatCreatesATeamFailsTheUpdate() {
        doAnswer(invocation -> {
            invocation.<ArchitectureModel>getArgument(0).addTeam(Team.builder().name("rogue-team").build());
            return null;
        }).when(importer).importIntoModel(any(ArchitectureModel.class), eq("dev"));

        String importerName = importer.getClass().getSimpleName();
        assertThatThrownBy(() -> service.runImporter(importerName))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("rogue-team");
    }

    @Test
    void anImporterThatRemovesASystemFailsTheUpdate() {
        when(systemRepository.findAll()).thenReturn(List.of(System.builder().name(SYSTEM_NAME).build()));
        doAnswer(invocation -> {
            ArchitectureModel model = invocation.getArgument(0);
            model.removeSystem(model.getSystems().getFirst());
            return null;
        }).when(importer).importIntoModel(any(ArchitectureModel.class), eq("dev"));

        String importerName = importer.getClass().getSimpleName();
        assertThatThrownBy(() -> service.runImporter(importerName))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(SYSTEM_NAME);
    }

    @Test
    void singleImporterDoesNotRunWhileAnotherInstanceHoldsTheModelUpdateLock() {
        // A single-importer run onboards and saves systems and teams just like the scheduled update, so the two
        // must never run at the same time - not on one instance, and not on two.
        jdbcTemplate.update("""
                        insert into shedlock (name, lock_until, locked_at, locked_by) values (?, ?, ?, ?)
                        on conflict (name) do update set lock_until = excluded.lock_until,
                            locked_at = excluded.locked_at, locked_by = excluded.locked_by""",
                UpdateService.MODEL_UPDATE_LOCK, Timestamp.from(Instant.now().plus(Duration.ofDays(1))),
                Timestamp.from(Instant.now()), "another-instance");

        service.runImporter(importer.getClass().getSimpleName());

        verifyNoInteractions(importer, modelOnboardingService);
    }

    @Test
    void singleImporterAlsoRunsOnboardingFirst() {
        service.runImporter(importer.getClass().getSimpleName());
        var order = inOrder(modelOnboardingService, importer, systemRepository);
        order.verify(modelOnboardingService).apply(any(ArchitectureModel.class));
        order.verify(systemRepository).saveAll(any());
        order.verify(importer).importIntoModel(any(ArchitectureModel.class), eq("dev"));
        order.verify(systemRepository).saveAll(any());
    }

}
