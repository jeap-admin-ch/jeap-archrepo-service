package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.docgen.DocumentationGenerator;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.persistence.ArchitectureModelRepository;
import ch.admin.bit.jeap.archrepo.web.ArchRepoConfigProperties;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Comparator.comparing;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateService {
    private LocalDateTime lastRunGenerateDoc = LocalDateTime.MIN;
    private LocalDateTime lastRunUpdateModel = LocalDateTime.MIN;
    private final ArchitectureModelRepository repository;
    private final DocumentationGenerator documentationGenerator;
    private final List<ArchRepoImporter> importers;
    private final MeterRegistry meterRegistry;
    private final ArchRepoConfigProperties archRepoConfigProperties;

    @Timed("archrepo_generate_documentation")
    @Scheduled(cron = "${archrepo.documentation-generator.update-schedule}")
    @SchedulerLock(name = "generate-documentation-task", lockAtLeastFor = "5s", lockAtMostFor = "2h")
    @Transactional
    public void generateDocumentation() {
        log.info("Starting scheduled documentation generation");
        LockAssert.assertLocked();
        ArchitectureModel architectureModel = repository.load();
        documentationGenerator.generate(architectureModel);
        lastRunGenerateDoc = LocalDateTime.now();
        log.info("Scheduled documentation generation done");
    }

    @Timed("archrepo_model_update")
    @Scheduled(cron = "${archrepo.update-schedule}")
    @SchedulerLock(name = "update-model-task", lockAtLeastFor = "5s", lockAtMostFor = "2h")
    @Transactional
    public void updateModel() {
        log.info("Starting scheduled model update");
        LockAssert.assertLocked();
        ArchitectureModel architectureModel = repository.load();
        importers.stream()
                .sorted(comparing(ArchRepoImporter::getOrder))
                .forEach(importer -> importer.importIntoModel(architectureModel, archRepoConfigProperties.getEnvironment().name().toLowerCase()));
        architectureModel.cleanup();
        repository.save(architectureModel);
        lastRunUpdateModel = LocalDateTime.now();

        log.info("Scheduled model update done");
    }

    @Transactional
    public void runImporter(String name) {
        ArchRepoImporter importer = importers.stream()
                .filter(i -> i.getClass().getSimpleName().equalsIgnoreCase(name))
                .findFirst().orElseThrow();

        log.info("Running importer {}", importer);
        ArchitectureModel architectureModel = repository.load();
        importer.importIntoModel(architectureModel, archRepoConfigProperties.getEnvironment().name().toLowerCase());
        repository.save(architectureModel);
        log.info("Import done");
    }

    private long calculateDaysFromLastRunToNow(LocalDateTime lastRun) {
        return Duration.between(lastRun, LocalDateTime.now()).toDays();
    }

    @PostConstruct
    private void createMonitoringMetrics() {
        Gauge.builder("archrepo_generate_documentation_last_run_from", () -> this.calculateDaysFromLastRunToNow(lastRunGenerateDoc))
                .baseUnit("days")
                .register(meterRegistry);

        Gauge.builder("archrepo_model_update_last_run_from", () -> this.calculateDaysFromLastRunToNow(lastRunUpdateModel))
                .baseUnit("days")
                .register(meterRegistry);
    }
}
