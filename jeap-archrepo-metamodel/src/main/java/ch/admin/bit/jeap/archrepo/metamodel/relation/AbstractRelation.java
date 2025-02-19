package ch.admin.bit.jeap.archrepo.metamodel.relation;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.Relation;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.*;

@NoArgsConstructor
@SuperBuilder
@Getter
@Entity(name = "relation")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class AbstractRelation implements Relation {

    @Id
    @NotNull
    @Getter
    private UUID id;

    @Setter
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
    private System definingSystem;

    // Reference providing/consuming components by name as provider/consumer might be external to the
    // system defining the relation
    private String providerName;
    private String consumerName;

    @EqualsAndHashCode.Exclude
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "relation_importers")
    private SortedSet<Importer> importers = new TreeSet<>();

    @NotNull
    @EqualsAndHashCode.Exclude
    @Enumerated(EnumType.STRING)
    private RelationStatus status;

    protected AbstractRelation(System definingSystem, String providerName, String consumerName, Importer importer) {
        this.id = UUID.randomUUID();
        this.definingSystem = definingSystem;
        this.providerName = providerName;
        this.consumerName = consumerName;
        this.importers = new TreeSet<>();
        addImporter(importer);
        this.status = RelationStatus.ACTIVE;
    }

    @Override
    public void addImporter(Importer importer){
        if (importer != null) {
            this.importers.add(importer);
        }
    }

    @Override
    public Set<Importer> getImporters(){
        return Collections.unmodifiableSet(this.importers);
    }

    public void markDeleted() {
        this.status = RelationStatus.DELETED;
    }
}
