package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.persistence.ArchitectureModelRepository;
import ch.admin.bit.jeap.archrepo.persistence.SchedulerRunRepository;
import ch.admin.bit.jeap.archrepo.persistence.SchedulerRunTracker;
import ch.admin.bit.jeap.archrepo.web.ArchRepoConfigProperties;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Comparator.comparing;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateService {
    /**
     * Shared by the scheduled update and a single-importer run: both onboard and save systems and teams, so they
     * must never run at the same time, on one instance or several.
     */
    static final String MODEL_UPDATE_LOCK = "update-model-task";

    private final ArchitectureModelRepository repository;
    private final List<ArchRepoImporter> importers;
    private final MeterRegistry meterRegistry;
    private final ArchRepoConfigProperties archRepoConfigProperties;
    private final SchedulerRunRepository schedulerRunRepository;
    private final ModelOnboardingService modelOnboardingService;

    private SchedulerRunTracker updateModelRunTracker;

    @PostConstruct
    void init() {
        updateModelRunTracker = new SchedulerRunTracker(
                "update-model",
                "archrepo_model_update_last_run_from",
                schedulerRunRepository,
                meterRegistry);
        updateModelRunTracker.init();
    }

    @Timed("archrepo_model_update")
    @Scheduled(cron = "${archrepo.update-schedule}")
    @SchedulerLock(name = MODEL_UPDATE_LOCK, lockAtLeastFor = "5s", lockAtMostFor = "2h")
    @Transactional
    public void updateModel() {
        log.info("Starting scheduled model update");
        LockAssert.assertLocked();
        ArchitectureModel architectureModel = loadOnboardedModel();
        importers.stream()
                .sorted(comparing(ArchRepoImporter::getOrder))
                .forEach(importer -> importer.importIntoModel(architectureModel, archRepoConfigProperties.getEnvironment().name().toLowerCase()));
        architectureModel.cleanup();
        repository.save(architectureModel);
        updateModelRunTracker.recordRun();

        log.info("Scheduled model update done");
    }

    // Skipped, not queued, while the lock is held - the endpoint triggering it is asynchronous anyway.
    @SchedulerLock(name = MODEL_UPDATE_LOCK, lockAtMostFor = "2h")
    @Transactional
    public void runImporter(String name) {
        ArchRepoImporter importer = importers.stream()
                .filter(i -> i.getClass().getSimpleName().equalsIgnoreCase(name))
                .findFirst().orElseThrow();

        log.info("Running importer {}", importer);
        ArchitectureModel architectureModel = loadOnboardedModel();
        importer.importIntoModel(architectureModel, archRepoConfigProperties.getEnvironment().name().toLowerCase());
        repository.save(architectureModel);
        log.info("Import done");
    }

    /**
     * The stored model with the configured systems and teams applied - and already saved, deletions included.
     * <p>
     * Saved before any importer runs, in the same transaction: an importer may create a component under a retained
     * system with the name of one that belonged to a system deleted by this onboarding (RHOS, for instance,
     * resolves the system from the namespace, not the component name). Component names are globally unique in the
     * database, and within a single flush Hibernate always inserts before it deletes, so the old row has to be gone
     * before an importer can add the new one. Saving here keeps that ordering out of {@link ModelOnboardingService},
     * which stays free of persistence. The second save after the importers finds the removed rows already deleted
     * and skips them.
     * <p>
     * Sealed right after onboarding: the configuration is the master of systems and teams, so an importer that
     * adds or removes one fails the update - even when onboarding is inactive, as nothing but the configuration may
     * create a system.
     */
    private ArchitectureModel loadOnboardedModel() {
        ArchitectureModel architectureModel = repository.load();
        modelOnboardingService.apply(architectureModel);
        architectureModel.sealSystemsAndTeams();
        repository.save(architectureModel);
        return architectureModel;
    }
}
