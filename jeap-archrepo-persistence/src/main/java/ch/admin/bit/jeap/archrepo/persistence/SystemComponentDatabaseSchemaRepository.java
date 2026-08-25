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

    @Query("""
            select s.system.name as system, s.systemComponent.name as component, s.schemaVersion as version,
                   s.contentHash as contentHash,
                   coalesce(s.modifiedAt, s.createdAt) as lastModifiedAt
            from SystemComponentDatabaseSchema s
            order by s.system.name, s.systemComponent.name
            """)
    List<ArtifactIndexEntry> findIndexEntries();

    @Query("""
            select s.system.name as system, s.systemComponent.name as component, s.schemaVersion as version,
                   s.contentHash as contentHash,
                   coalesce(s.modifiedAt, s.createdAt) as lastModifiedAt
            from SystemComponentDatabaseSchema s
            where lower(s.system.name) = lower(:systemName)
            order by s.systemComponent.name
            """)
    List<ArtifactIndexEntry> findIndexEntriesBySystemName(String systemName);

}
