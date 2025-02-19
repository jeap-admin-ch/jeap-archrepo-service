package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemRepository extends JpaRepository<System, UUID> {

    Optional<System> findByNameContainingIgnoreCase(String name);

}
