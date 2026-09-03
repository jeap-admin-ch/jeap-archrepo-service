package ch.admin.bit.jeap.archrepo.metamodel.database;

import ch.admin.bit.jeap.archrepo.metamodel.ContentHash;
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

    /**
     * SHA-256 of {@link #schema}, kept so that the docs API can serve an entity tag and answer a conditional
     * request without reading the blob. Null for schemas stored before the column existed; those are backfilled
     * lazily on first read.
     */
    private String contentHash;

    @Builder
    @SuppressWarnings("unused")
    public SystemComponentDatabaseSchema(@NonNull SystemComponent systemComponent, byte[] schema, @NonNull String schemaVersion) {
        Objects.requireNonNull(schema, "schema cannot be null");
        this.id = UUID.randomUUID();
        this.system = systemComponent.getParent();
        this.systemComponent = systemComponent;
        this.schema = schema;
        this.contentHash = ContentHash.of(schema);
        this.schemaVersion = schemaVersion;
    }

    public SystemComponentDatabaseSchema update(byte[] schema, String version) {
        Objects.requireNonNull(schema, "schema cannot be null");
        Objects.requireNonNull(version, "version cannot be null");
        this.schema = schema;
        this.contentHash = ContentHash.of(schema);
        this.schemaVersion = version;
        return this;
    }

    /**
     * Hand-written rather than generated, and it must stay free of the associations and the blob: {@code system}
     * is a lazy {@code @ManyToOne} whose {@code getName()} initialises the proxy, and {@code schema} is a lazy
     * blob. This is rendered from the exception messages of {@code System.addDatabaseSchema}, so it has to work
     * on a detached instance too. The component name is safe - that association is eager.
     */
    @Override
    public String toString() {
        return "SystemComponentDatabaseSchema{" +
               "id=" + id +
               ", systemComponent=" + systemComponent.getName() +
               ", schemaVersion=" + schemaVersion +
               '}';
    }

}
