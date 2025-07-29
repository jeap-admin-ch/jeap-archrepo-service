package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class OpenApiSpecRepositoryTest {

    private static final String SYSTEM_NAME = "test-system";
    private static final String COMPONENT_NAME = "test-component";

    @Autowired
    private OpenApiSpecRepository openApiSpecRepository;

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void saveOpenApiSpec_and_find() {
        final SystemComponent component = createPersistentSystemComponent();
        final OpenApiSpec openApiSpec = createOpenApiSpec(component, "1.2.3");

        openApiSpecRepository.saveAndFlush(openApiSpec);

        assertThat(openApiSpecRepository.findAll()).hasSize(1);

        Optional<OpenApiSpec> openApiSpecOptional = openApiSpecRepository.
                findByProvider(openApiSpec.getProvider());

        assertThat(openApiSpecOptional).isPresent();
    }

    @Test
    void testFindApiDocVersions() {
        final String version = "1.2.3";
        final SystemComponent component = createPersistentSystemComponent();
        final OpenApiSpec openApiSpec = createOpenApiSpec(component, version);
        openApiSpecRepository.saveAndFlush(openApiSpec);

        List<ApiDocVersion> apiDocVersions = openApiSpecRepository.getApiDocVersions();

        assertThat(apiDocVersions).hasSize(1);
        assertThat(apiDocVersions.getFirst().getSystem()).isEqualTo(SYSTEM_NAME);
        assertThat(apiDocVersions.getFirst().getComponent()).isEqualTo(COMPONENT_NAME);
        assertThat(apiDocVersions.getFirst().getVersion()).isEqualTo(version);
    }

    @Test
    void testFindApiDocVersion_found() {
        final String version = "1.2.3";
        final SystemComponent component = createPersistentSystemComponent();
        openApiSpecRepository.saveAndFlush(createOpenApiSpec(component, version));

        Optional<ApiDocDto> apiDoc = openApiSpecRepository.getApiDocVersion(component);

        assertThat(apiDoc).isPresent();
        assertThat(apiDoc.get().getVersion()).isEqualTo(version);
        assertThat(apiDoc.get().getCreatedAt()).isNotNull();
        assertThat(apiDoc.get().getModifiedAt()).isNull();
    }

    @Test
    void testFindApiDocVersion_modified_found() {
        final String version = "1.2.3";
        final SystemComponent component = createPersistentSystemComponent();
        OpenApiSpec openApiSpec = openApiSpecRepository.saveAndFlush(createOpenApiSpec(component, version));
        openApiSpec.update("foo".getBytes(StandardCharsets.UTF_8), "1.2.4", "url2");

        Optional<ApiDocDto> apiDoc = openApiSpecRepository.getApiDocVersion(component);
        assertThat(apiDoc).isPresent();
        assertThat(apiDoc.get().getVersion()).isEqualTo("1.2.4");
        assertThat(apiDoc.get().getCreatedAt()).isNotNull();
        assertThat(apiDoc.get().getModifiedAt()).isNotNull();
        assertThat(apiDoc.get().getServerUrl()).isEqualTo("url2");
    }

    @Test
    void testFindApiDocVersion_notFound() {
        final SystemComponent component = createPersistentSystemComponent();
        Optional<ApiDocDto> apiDoc = openApiSpecRepository.getApiDocVersion(component);
        assertThat(apiDoc).isEmpty();
    }

    @SuppressWarnings("SameParameterValue")
    private OpenApiSpec createOpenApiSpec(SystemComponent component, String version) {
        return OpenApiSpec.builder()
                .provider(component)
                .content("test".getBytes(StandardCharsets.UTF_8))
                .version(version)
                .serverUrl("url1")
                .build();
    }

    private SystemComponent createPersistentSystemComponent() {
        Team team = teamRepository.save(Team.builder().name("team").build());
        System system = System.builder().name(SYSTEM_NAME).defaultOwner(team).build();
        SystemComponent systemComponent = BackendService.builder()
                .id(UUID.randomUUID())
                .name(COMPONENT_NAME)
                .ownedBy(team)
                .build();
        ReflectionTestUtils.setField(systemComponent, "createdAt", ZonedDateTime.now());
        system.addSystemComponent(systemComponent);
        systemRepository.save(system);
        return systemComponent;
    }

}

