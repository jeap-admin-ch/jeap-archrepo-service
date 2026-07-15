package ch.admin.bit.jeap.archrepo.metamodel.message;

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
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "graph_data", columnDefinition = "BYTEA")
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

    /**
     * Returns the canonical variant key shared by persistence and graph deep-link navigation. Default aliases are
     * normalized to an empty key, while an optional message-type prefix is removed case-insensitively so producers,
     * stored graphs, and navigation URLs use the same identifier.
     */
    public static String normalizeVariant(String messageTypeName, String variant) {
        if (variant == null) {
            return "";
        }
        String normalized = variant.trim();
        if (normalized.isEmpty() || "default".equalsIgnoreCase(normalized) ||
                messageTypeName != null && normalized.equalsIgnoreCase(messageTypeName)) {
            return "";
        }
        String messageTypePrefix = messageTypeName == null ? null : messageTypeName + "/";
        if (messageTypePrefix != null && normalized.regionMatches(true, 0, messageTypePrefix, 0, messageTypePrefix.length())) {
            normalized = normalized.substring(messageTypePrefix.length()).trim();
        }
        return "default".equalsIgnoreCase(normalized) ||
                messageTypeName != null && normalized.equalsIgnoreCase(messageTypeName) ? "" : normalized;
    }
}
