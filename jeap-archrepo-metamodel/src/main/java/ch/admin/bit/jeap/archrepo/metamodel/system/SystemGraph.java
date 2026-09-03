package ch.admin.bit.jeap.archrepo.metamodel.system;

import ch.admin.bit.jeap.archrepo.metamodel.MutableDomainEntity;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
// Include-only so that the graph blob can never reach a log line - see EntityToStringDoesNotQueryTest
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Getter
@Table(name = "system_graph")
public class SystemGraph extends MutableDomainEntity {

    @Id
    @NotNull
    @ToString.Include
    private UUID id;

    @NotNull
    @Column(name = "system_name")
    @ToString.Include
    private String systemName;

    @Lob
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "graph_data", columnDefinition = "BYTEA")
    private byte[] graphData;

    @ToString.Include
    private String fingerprint;

    protected SystemGraph() {
        super();
    }

    @Builder
    public SystemGraph(String systemName, byte[] graphData, String fingerprint) {
        this.id = Generators.timeBasedEpochGenerator().generate();
        this.systemName = systemName;
        this.graphData = graphData;
        this.fingerprint = fingerprint;
    }
}
