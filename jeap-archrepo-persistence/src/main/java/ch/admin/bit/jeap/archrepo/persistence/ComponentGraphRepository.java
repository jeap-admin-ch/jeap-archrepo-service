package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.system.ComponentGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface ComponentGraphRepository extends JpaRepository<ComponentGraph, UUID> {

    boolean existsBySystemNameAndComponentName(String systemName, String componentName);

    @Modifying
    @Transactional
    @Query(value = "UPDATE component_graph SET graph_data = :graphData, fingerprint = :fingerprint, modified_at = CURRENT_TIMESTAMP WHERE system_name = :systemName AND component_name = :componentName AND fingerprint != :fingerprint", nativeQuery = true)
    void updateGraphAndFingerprintBySystemNameAndComponentNameIfFingerprintChanged(String systemName, String componentName, byte[] graphData, String fingerprint);
}
