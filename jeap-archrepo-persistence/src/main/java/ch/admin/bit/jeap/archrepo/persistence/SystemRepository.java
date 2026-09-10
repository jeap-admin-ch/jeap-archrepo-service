package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemRepository extends JpaRepository<System, UUID> {

    Optional<System> findByNameContainingIgnoreCase(String name);

    Optional<System> findByNameIgnoreCase(String name);

    @Query("SELECT s FROM System s JOIN s.aliases a WHERE LOWER(a) = LOWER(:name) ORDER BY s.name")
    List<System> findAllByAliasIgnoreCase(@Param("name") String name);

    /**
     * The system of that name, or - only where no system carries the name - one holding it as an alias.
     * <p>
     * The alias lookup returns a list because nothing stops two systems from carrying the same alias: as a
     * single-result query it threw for every caller as soon as one did, and a lookup that fails on ambiguous
     * data is worse than one that answers the same system every time. New aliases like that are refused, the
     * ones that predate the refusal stay resolvable.
     */
    default Optional<System> findByNameOrAliasIgnoreCase(String name) {
        return findByNameIgnoreCase(name)
                .or(() -> findAllByAliasIgnoreCase(name).stream().findFirst());
    }

}
