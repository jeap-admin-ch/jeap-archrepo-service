package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemRepository extends JpaRepository<System, UUID> {

    Optional<System> findByNameContainingIgnoreCase(String name);

    Optional<System> findByNameIgnoreCase(String name);

    @Query("SELECT s FROM System s JOIN s.aliases a WHERE LOWER(a) = LOWER(:name)")
    Optional<System> findByAliasIgnoreCase(@Param("name") String name);

    default Optional<System> findByNameOrAliasIgnoreCase(String name) {
        return findByNameIgnoreCase(name).or(() -> findByAliasIgnoreCase(name));
    }

}
