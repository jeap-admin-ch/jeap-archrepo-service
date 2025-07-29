package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class RestApiRepositoryTest {

    @Autowired
    private RestApiRepository repository;

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void findByDefiningSystemAndProvider() {
        Team team = teamRepository.save(Team.builder().name("team").build());

        System systemA =  System.builder()
                .name("systemA")
                .defaultOwner(team)
                .build();
        System systemB =  System.builder()
                .name("systemB")
                .defaultOwner(team)
                .build();
        BackendService providerA = BackendService.builder()
                .name("providerA")
                .parent(systemA)
                .id(UUID.randomUUID())
                .ownedBy(team)
                .createdAt(ZonedDateTime.now())
                .build();
        systemA.addSystemComponent(providerA);

        BackendService providerB = BackendService.builder()
                .name("providerB")
                .parent(systemA)
                .id(UUID.randomUUID())
                .ownedBy(team)
                .createdAt(ZonedDateTime.now())
                .build();
        systemA.addSystemComponent(providerB);

        systemRepository.save(systemA);

        BackendService providerC = BackendService.builder()
                .name("providerC")
                .parent(systemA)
                .id(UUID.randomUUID())
                .ownedBy(team)
                .createdAt(ZonedDateTime.now())
                .build();
        systemB.addSystemComponent(providerC);
        systemRepository.save(systemB);

        repository.save(new RestApi(providerA, "get", "path1", Importer.GRAFANA));
        repository.save(new RestApi(providerA, "post", "path1", Importer.GRAFANA));
        repository.save(new RestApi(providerA, "get", "path2", Importer.GRAFANA));
        repository.save(new RestApi(providerB, "get", "path2", Importer.GRAFANA));
        repository.save(new RestApi(providerC, "delete", "path3", Importer.GRAFANA));

        List<RestApi> providerAApis = repository.findByProvider(providerA);
        assertThat(providerAApis).hasSize(3);

        List<RestApi> providerBApis = repository.findByProvider(providerB);
        assertThat(providerBApis).hasSize(1);

        List<RestApi> providerCApis = repository.findByProvider(providerC);
        assertThat(providerCApis).hasSize(1);
        assertThat(providerCApis.getFirst().getPath()).isEqualTo("path3");
        assertThat(providerCApis.getFirst().getMethod()).isEqualTo("DELETE");

    }

}
