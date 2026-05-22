package ch.admin.bit.jeap.archrepo.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface JpaSchedulerRunRepository extends JpaRepository<SchedulerRun, String> {
}
