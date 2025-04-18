package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import java.util.List;
import java.util.UUID;

@Repository
public interface RestApiRepository extends JpaRepository<RestApi, UUID> {

    List<RestApi> findByDefiningSystemAndProvider(System definingSystem, SystemComponent provider);

}
