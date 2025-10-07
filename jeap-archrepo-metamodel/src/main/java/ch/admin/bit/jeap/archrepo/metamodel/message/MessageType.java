package ch.admin.bit.jeap.archrepo.metamodel.message;

import ch.admin.bit.jeap.archrepo.metamodel.Importable;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.*;

@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class MessageType implements Importable {

    @Id
    @NotNull
    @Getter
    private UUID id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
    System parent;

    @Setter
    @Enumerated(EnumType.STRING)
    private Importer importer;

    @NonNull
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "message_type_versions")
    private List<MessageVersion> messageVersions;

    private String topic;
    @NonNull
    private String scope;
    @NonNull
    private String messageTypeName;
    @NonNull
    private String descriptorUrl;
    private String description;
    private String documentationUrl;

    public void setDocumentationUrl(String documentationUrl) {
        if (documentationUrl == null || documentationUrl.isEmpty()) {
            return;
        }
        this.documentationUrl = documentationUrl;
    }

    public abstract Set<String> getComponentNamesWithContract();
}
