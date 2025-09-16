package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface MessageGraphRepository extends JpaRepository<MessageGraph, UUID> {

    boolean existsByMessageTypeNameAndVariant(String messageTypeName, String variant);

    @Modifying
    @Transactional
    @Query(value = "UPDATE message_graph SET graph_data = :graphData, fingerprint = :fingerprint, modified_at = CURRENT_TIMESTAMP WHERE message_type_name = :messageTypeName AND variant = :variant AND fingerprint != :fingerprint", nativeQuery = true)
    void updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(String messageTypeName, String variant, byte[] graphData, String fingerprint);
}
