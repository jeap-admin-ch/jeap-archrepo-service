package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class SystemComponentDatabaseSchemaRepositoryTest {

    private static final String SYSTEM_NAME = "test-system";
    private static final String COMPONENT_NAME = "test-component";
    private static final String SCHEMA_VERSION = "1.2.3";
    private static final byte[] SERIALIZED_SCHEMA = "some-schema-content".getBytes();

    @Autowired
    private SystemComponentDatabaseSchemaRepository repository;

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void testSaveAndFind() {
        final SystemComponent systemComponent = createPersistentSystemComponent();
        final SystemComponentDatabaseSchema dbSchema =
                createSystemComponentDatabaseSchema(systemComponent, SCHEMA_VERSION, SERIALIZED_SCHEMA);

        repository.saveAndFlush(dbSchema);
        List<SystemComponentDatabaseSchema> allDbSchemas = repository.findAll();

        assertThat(allDbSchemas).hasSize(1);
        SystemComponentDatabaseSchema dbSchemaRead = allDbSchemas.getFirst();
        assertThat(dbSchemaRead.getSystemComponent().getId()).isEqualTo(systemComponent.getId());
        assertThat(dbSchemaRead.getSystem().getId()).isEqualTo(systemComponent.getParent().getId());
        assertThat(dbSchemaRead.getSchemaVersion()).isEqualTo(SCHEMA_VERSION);
        assertThat(dbSchemaRead.getSchema()).isEqualTo(SERIALIZED_SCHEMA);
        assertThat(dbSchemaRead.getId()).isEqualTo(dbSchema.getId());
        assertThat(dbSchemaRead).isEqualTo(dbSchema);
    }

    @Test
    void testFindBySystemComponent() {
        final SystemComponent systemComponent = createPersistentSystemComponent();
        final SystemComponentDatabaseSchema dbSchema =
                createSystemComponentDatabaseSchema(systemComponent, SCHEMA_VERSION, SERIALIZED_SCHEMA);
        repository.saveAndFlush(dbSchema);
        assertThat(repository.findAll()).hasSize(1);

        Optional<SystemComponentDatabaseSchema> dbSchemaRead = repository.findBySystemComponent(systemComponent);

        assertThat(dbSchemaRead).isPresent();
        assertThat(dbSchemaRead.get()).isEqualTo(dbSchema);
    }

    @SuppressWarnings("SameParameterValue")
    private SystemComponentDatabaseSchema createSystemComponentDatabaseSchema(SystemComponent component, String version, byte[] schema) {
        return SystemComponentDatabaseSchema.builder()
                .systemComponent(component)
                .schemaVersion(version)
                .schema(schema)
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

