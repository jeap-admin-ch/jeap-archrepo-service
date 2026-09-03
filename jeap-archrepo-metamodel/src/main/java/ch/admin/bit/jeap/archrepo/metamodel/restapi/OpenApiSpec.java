package ch.admin.bit.jeap.archrepo.metamodel.restapi;

import ch.admin.bit.jeap.archrepo.metamodel.ContentHash;
import ch.admin.bit.jeap.archrepo.metamodel.MutableDomainEntity;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Getter
public class OpenApiSpec extends MutableDomainEntity {

    @Id
    @NotNull
    @ToString.Include
    private UUID id;

    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
    @Setter
    private System definingSystem;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private SystemComponent provider;

    @ToString.Include
    private String version;

    @ToString.Include
    private String serverUrl;

    private byte[] content;

    /**
     * SHA-256 of {@link #content}, kept so that the docs API can serve an entity tag and answer a conditional
     * request without reading the blob. Null for specs stored before the column existed; those are backfilled
     * lazily on first read.
     */
    private String contentHash;

    @Builder
    @SuppressWarnings("unused")
    public OpenApiSpec(@NonNull SystemComponent provider, String version, byte[] content, String serverUrl) {
        this.id = UUID.randomUUID();
        this.definingSystem = provider.getParent();
        this.provider = provider;
        this.version = version;
        this.content = content;
        this.contentHash = ContentHash.of(content);
        this.serverUrl = serverUrl;
    }

    public void update(byte[] content, String version, String serverUrl) {
        this.content = content;
        this.contentHash = ContentHash.of(content);
        this.version = version;
        this.serverUrl = serverUrl;
    }

}
