package ch.admin.bit.jeap.archrepo.metamodel.message;

import ch.admin.bit.jeap.archrepo.metamodel.MutableDomainEntity;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@ToString
@Entity
@Getter
@Table(name = "message_graph")
public class MessageGraph extends MutableDomainEntity {

    @Id
    @NotNull
    private UUID id;

    @NotNull
    @Column(name = "message_type_name")
    private String messageTypeName;

    @NotNull
    @Column(name = "variant")
    private String variant;

    @Lob
    @Column(name = "graph_data")
    private byte[] graphData;

    private String fingerprint;

    protected MessageGraph() {
        super();
    }

    @Builder
    public MessageGraph(String messageTypeName, String variant, byte[] graphData, String fingerprint) {
        this.id = Generators.timeBasedEpochGenerator().generate();
        this.messageTypeName = messageTypeName;
        this.variant = variant;
        this.graphData = graphData;
        this.fingerprint = fingerprint;
    }
}
