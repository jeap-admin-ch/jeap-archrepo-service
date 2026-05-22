package ch.admin.bit.jeap.archrepo.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SchedulerRunRepository {

    Optional<LocalDateTime> findLastRunDateTime(String jobName);

    void saveLastRunDateTime(String jobName, LocalDateTime lastRunDateTime);
}
