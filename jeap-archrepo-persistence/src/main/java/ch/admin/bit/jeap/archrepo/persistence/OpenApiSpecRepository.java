package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpenApiSpecRepository extends JpaRepository<OpenApiSpec, UUID> {

    Optional<OpenApiSpec> findByProvider(SystemComponent provider);

    @Query("select s.definingSystem.name as system, s.provider.name as component, s.version as version from OpenApiSpec s")
    List<ApiDocVersion> getApiDocVersions();

    @Query("select serverUrl as serverUrl, version as version, createdAt as createdAt, modifiedAt as modifiedAt from OpenApiSpec where provider = :provider")
    Optional<ApiDocDto> getApiDocVersion(SystemComponent provider);

}
