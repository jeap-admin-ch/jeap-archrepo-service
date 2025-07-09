package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemComponentRepository extends JpaRepository<SystemComponent, UUID> {

    Optional<SystemComponent> findByNameIgnoreCase(String name);

}
