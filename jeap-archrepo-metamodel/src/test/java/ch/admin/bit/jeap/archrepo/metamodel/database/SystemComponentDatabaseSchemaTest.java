package ch.admin.bit.jeap.archrepo.metamodel.database;

import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemComponentDatabaseSchemaTest {

    private static final byte[] SCHEMA = "dummy_schema".getBytes();
    private static final String SCHEMA_VERSION = "1.2.3";


    @Test
    void testBuilder() {
        final SystemComponent systemComponent = mockSystemComponent();

        final SystemComponentDatabaseSchema dbSchema =
                createSystemComponentDatabaseSchema(systemComponent);

        assertThat(dbSchema.getSystem()).isEqualTo(systemComponent.getParent());
        assertThat(dbSchema.getId()).isNotNull(); // created at construction time
        assertThat(dbSchema.getSystemComponent()).isEqualTo(systemComponent);
        assertThat(dbSchema.getSchemaVersion()).isEqualTo(SCHEMA_VERSION);
        assertThat(dbSchema.getSchema()).isEqualTo(SCHEMA);
    }

    @Test
    void testUpdateValid() {
        final SystemComponent systemComponent = mockSystemComponent();
        final SystemComponentDatabaseSchema dbSchema =
                createSystemComponentDatabaseSchema(systemComponent);
        final byte[] newSchema = "new_schema".getBytes();
        final String newSchemaVersion = "4.5.6";

        dbSchema.update(newSchema, newSchemaVersion);

        assertThat(dbSchema.getSchema()).isEqualTo(newSchema);
        assertThat(dbSchema.getSchemaVersion()).isEqualTo(newSchemaVersion);
    }

    @Test
    void testUpdateInvalid() {
        final SystemComponent systemComponent = mockSystemComponent();
        final SystemComponentDatabaseSchema dbSchema =
                createSystemComponentDatabaseSchema(systemComponent);
        final byte[] newSchema = "new_schema".getBytes();
        final String newSchemaVersion = "4.5.6";

        assertThatThrownBy(() -> dbSchema.update(null, newSchemaVersion))
                .hasMessage("schema cannot be null");
        assertThatThrownBy(() -> dbSchema.update(newSchema, null))
                .hasMessage("version cannot be null");
    }

    @Test
    void testToString() {
        final System system = mock(System.class);
        final SystemComponent systemComponent = mock(SystemComponent.class);
        when(system.getName()).thenReturn("test-system");
        when(systemComponent.getParent()).thenReturn(system);
        when(systemComponent.getName()).thenReturn("test-system-component");
        final SystemComponentDatabaseSchema dbSchema = SystemComponentDatabaseSchema.builder()
                .schema("dummy_schema".getBytes())
                .schemaVersion("1.2.3")
                .systemComponent(systemComponent)
                .build();

        final String expectedString = "SystemComponentDatabaseSchema{" +
                "id=" + dbSchema.getId() +
                ", system=test-system" +
                ", systemComponent=test-system-component" +
                ", schemaVersion=1.2.3" +
                '}';
        assertThat(dbSchema.toString()).isEqualTo(expectedString);
    }

    private SystemComponent mockSystemComponent() {
        System system = mock(System.class);
        SystemComponent systemComponent = mock(SystemComponent.class);
        when(systemComponent.getParent()).thenReturn(system);
        return systemComponent;
    }

    private SystemComponentDatabaseSchema createSystemComponentDatabaseSchema(SystemComponent systemComponent) {
        return SystemComponentDatabaseSchema.builder()
                .schema(SCHEMA)
                .schemaVersion(SCHEMA_VERSION)
                .systemComponent(systemComponent)
                .build();
    }

}



