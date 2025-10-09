package ch.admin.bit.jeap.archrepo.metamodel.system;

import ch.admin.bit.jeap.archrepo.metamodel.MutableDomainEntity;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@ToString
@Entity
@Getter
@Table(name = "system_graph")
public class SystemGraph extends MutableDomainEntity {

    @Id
    @NotNull
    private UUID id;

    @NotNull
    @Column(name = "system_name")
    private String systemName;

    @Lob
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "graph_data", columnDefinition = "BYTEA")
    private byte[] graphData;

    private String fingerprint;

    @Column(name = "last_published_fingerprint")
    private String lastPublishedFingerprint;

    protected SystemGraph() {
        super();
    }

    @Builder
    public SystemGraph(String systemName, byte[] graphData, String fingerprint, String lastPublishedFingerprint) {
        this.id = Generators.timeBasedEpochGenerator().generate();
        this.systemName = systemName;
        this.graphData = graphData;
        this.fingerprint = fingerprint;
        this.lastPublishedFingerprint = lastPublishedFingerprint;
    }
}