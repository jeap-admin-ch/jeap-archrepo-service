package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.system.SystemGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface SystemGraphRepository extends JpaRepository<SystemGraph, UUID> {

    boolean existsBySystemName(String systemName);

    @Modifying
    @Transactional
    @Query(value = "UPDATE system_graph SET graph_data = :graphData, fingerprint = :fingerprint, modified_at = CURRENT_TIMESTAMP WHERE system_name = :systemName AND fingerprint != :fingerprint", nativeQuery = true)
    void updateGraphDataAndFingerprintIfFingerprintChanged(String systemName, byte[] graphData, String fingerprint);

    SystemGraph findBySystemNameIgnoreCase(String systemName);

    @Modifying
    @Transactional
    @Query("UPDATE SystemGraph g SET g.lastPublishedFingerprint = :fingerprint WHERE g.id = :id")
    void updateLastPublishedFingerprint(UUID id, String fingerprint);
}
