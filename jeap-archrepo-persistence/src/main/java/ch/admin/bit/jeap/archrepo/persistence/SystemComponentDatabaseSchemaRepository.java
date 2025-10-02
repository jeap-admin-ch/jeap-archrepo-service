package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemComponentDatabaseSchemaRepository extends JpaRepository<SystemComponentDatabaseSchema, UUID> {

    Optional<SystemComponentDatabaseSchema> findBySystemComponent(SystemComponent systemComponent);

    @Query("select s.system.name as system, s.systemComponent.name as component, s.schemaVersion as version from SystemComponentDatabaseSchema s")
    List<DatabaseSchemaVersion> getDatabaseSchemaVersions();

}
