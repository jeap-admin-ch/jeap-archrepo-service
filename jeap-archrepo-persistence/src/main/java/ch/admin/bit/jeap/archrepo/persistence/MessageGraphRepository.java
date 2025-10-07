package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageGraphRepository extends JpaRepository<MessageGraph, UUID> {

    boolean existsByMessageTypeNameAndVariant(String messageTypeName, String variant);

    @Modifying
    @Transactional
    @Query(value = "UPDATE message_graph SET graph_data = :graphData, fingerprint = :fingerprint, modified_at = CURRENT_TIMESTAMP WHERE message_type_name = :messageTypeName AND variant = :variant AND fingerprint != :fingerprint", nativeQuery = true)
    void updateGraphAndFingerprintByMessageTypeNameAndVariantIfFingerprintChanged(String messageTypeName, String variant, byte[] graphData, String fingerprint);

    List<MessageGraph> findAllByMessageTypeName(@NonNull String messageTypeName);

    @Modifying
    @Transactional
    @Query("UPDATE MessageGraph g SET g.actualDocFingerprint = :fingerprint WHERE g.id = :id")
    void updateActualDocFingerprint(UUID id, String fingerprint);
}
