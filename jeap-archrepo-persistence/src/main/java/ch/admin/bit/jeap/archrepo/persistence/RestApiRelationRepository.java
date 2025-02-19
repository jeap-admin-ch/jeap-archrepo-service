package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationStatus;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import java.util.List;
import java.util.UUID;

@Repository
public interface RestApiRelationRepository extends JpaRepository<RestApiRelation, UUID> {

    List<RestApiRelation> findAllByDefiningSystemAndProviderNameAndConsumerNameAndStatus(System definingSystem, String providerName, String consumerName, RelationStatus status);

}
