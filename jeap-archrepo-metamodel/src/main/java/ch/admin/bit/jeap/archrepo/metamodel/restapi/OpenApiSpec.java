package ch.admin.bit.jeap.archrepo.metamodel.restapi;

import ch.admin.bit.jeap.archrepo.metamodel.MutableDomainEntity;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@NoArgsConstructor
@ToString
@Entity
@Getter
public class OpenApiSpec extends MutableDomainEntity {

    @Id
    @NotNull
    private UUID id;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
    @Setter
    private System definingSystem;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @ToString.Exclude
    private SystemComponent provider;

    private String version;

    private String serverUrl;

    @ToString.Exclude
    private byte[] content;

    @Builder
    @SuppressWarnings("unused")
    public OpenApiSpec(@NonNull SystemComponent provider, String version, byte[] content, String serverUrl) {
        this.id = UUID.randomUUID();
        this.definingSystem = provider.getParent();
        this.provider = provider;
        this.version = version;
        this.content = content;
        this.serverUrl = serverUrl;
    }

    public void update(byte[] content, String version, String serverUrl) {
        this.content = content;
        this.version = version;
        this.serverUrl = serverUrl;
    }

}
