package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.system.ComponentGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComponentGraphRepository extends JpaRepository<ComponentGraph, UUID> {

    boolean existsBySystemNameAndComponentName(String systemName, String componentName);

    @Modifying
    @Transactional
    @Query(value = "UPDATE component_graph SET graph_data = :graphData, fingerprint = :fingerprint, modified_at = CURRENT_TIMESTAMP WHERE system_name = :systemName AND component_name = :componentName AND fingerprint != :fingerprint", nativeQuery = true)
    void updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(String systemName, String componentName, byte[] graphData, String fingerprint);

    ComponentGraph findByComponentNameIgnoreCase(String componentName);

    @Modifying
    @Transactional
    @Query("UPDATE ComponentGraph g SET g.lastPublishedFingerprint = :fingerprint WHERE g.id = :id")
    void updateLastPublishedFingerprint(UUID id, String fingerprint);


    @Query("""
    SELECT g.componentName AS component,
           MAX(g.createdAt) AS maxCreatedAt,
           MAX(g.modifiedAt) AS maxModifiedAt
    FROM ComponentGraph g
    GROUP BY g.componentName
    """)
    List<ReactionLastModifiedAt> getMaxCreatedAndModifiedAtList();

}
