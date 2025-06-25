package ch.admin.bit.jeap.archrepo.metamodel.database;

import ch.admin.bit.jeap.archrepo.metamodel.MutableDomainEntity;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA
public class SystemComponentDatabaseSchema extends MutableDomainEntity {

    @Id
    @NotNull
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
    @Setter
    private System system;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "system_component_id")
    private SystemComponent systemComponent;

    @NotNull
    @Basic(fetch = FetchType.LAZY)
    private byte[] schema;

    @NotNull
    private String schemaVersion;

    @Builder
    @SuppressWarnings("unused")
    public SystemComponentDatabaseSchema(@NonNull SystemComponent systemComponent, byte[] schema, @NonNull String schemaVersion) {
        Objects.requireNonNull(schema, "schema cannot be null");
        this.id = UUID.randomUUID();
        this.system = systemComponent.getParent();
        this.systemComponent = systemComponent;
        this.schema = schema;
        this.schemaVersion = schemaVersion;
    }

    public SystemComponentDatabaseSchema update(byte[] schema, String version) {
        Objects.requireNonNull(schema, "schema cannot be null");
        Objects.requireNonNull(version, "version cannot be null");
        this.schema = schema;
        this.schemaVersion = version;
        return this;
    }

    public String toString() {
        return "SystemComponentDatabaseSchema{" +
                "id=" + id +
                ", system=" + (system != null ? system.getName() : "null") +
                ", systemComponent=" + systemComponent.getName() +
                ", schemaVersion=" + schemaVersion +
                '}';
    }

}
