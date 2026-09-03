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
@Table(name = "component_graph")
public class ComponentGraph extends MutableDomainEntity {

    @Id
    @NotNull
    @ToString.Include
    private UUID id;

    @NotNull
    @Column(name = "system_name")
    @ToString.Include
    private String systemName;

    @NotNull
    @Column(name = "component_name")
    @ToString.Include
    private String componentName;

    @Lob
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "graph_data", columnDefinition = "BYTEA")
    private byte[] graphData;

    @ToString.Include
    private String fingerprint;

    protected ComponentGraph() {
        super();
    }

    @Builder
    public ComponentGraph(String systemName, String componentName, byte[] graphData, String fingerprint) {
        this.id = Generators.timeBasedEpochGenerator().generate();
        this.systemName = systemName;
        this.componentName = componentName;
        this.graphData = graphData;
        this.fingerprint = fingerprint;
    }
}
