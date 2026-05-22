package ch.admin.bit.jeap.archrepo.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
@Import(SchedulerRunRepositoryImpl.class)
class SchedulerRunRepositoryImplTest {

    @Autowired
    private SchedulerRunRepositoryImpl repository;

    @Test
    void findLastRunDateTime_returnsEmpty_whenNoRunPersisted() {
        Optional<LocalDateTime> result = repository.findLastRunDateTime("generate-documentation");

        assertThat(result).isEmpty();
    }

    @Test
    void saveAndFindLastRunDateTime() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        repository.saveLastRunDateTime("generate-documentation", now);

        Optional<LocalDateTime> result = repository.findLastRunDateTime("generate-documentation");
        assertThat(result).contains(now);
    }

    @Test
    void saveLastRunDateTime_updatesExistingRow() {
        LocalDateTime first = LocalDateTime.of(2025, 1, 15, 10, 0);
        LocalDateTime second = LocalDateTime.of(2025, 6, 20, 14, 30);

        repository.saveLastRunDateTime("update-model", first);
        repository.saveLastRunDateTime("update-model", second);

        Optional<LocalDateTime> result = repository.findLastRunDateTime("update-model");
        assertThat(result).contains(second);
    }

    @Test
    void saveLastRunDateTime_differentJobsAreIndependent() {
        LocalDateTime docTime = LocalDateTime.of(2025, 3, 10, 8, 0);
        LocalDateTime modelTime = LocalDateTime.of(2025, 3, 10, 12, 0);

        repository.saveLastRunDateTime("generate-documentation", docTime);
        repository.saveLastRunDateTime("update-model", modelTime);

        assertThat(repository.findLastRunDateTime("generate-documentation")).contains(docTime);
        assertThat(repository.findLastRunDateTime("update-model")).contains(modelTime);
    }
}
