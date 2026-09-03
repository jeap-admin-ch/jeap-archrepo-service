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
/*
 * Include-only. The subtypes are logged as whole objects by the relation importers and carry
 * @ToString(callSuper = true), so whatever this prints ends up in those log lines: it must stay the plain
 * columns of the relation row. providerName is deliberately out - RestApiRelation overrides its getter to
 * derive the name from its REST API, and Lombok renders a field through its getter.
 */
@ToString(onlyExplicitlyIncluded = true)
@Entity(name = "relation")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class AbstractRelation implements Relation {

    @Id
    @NotNull
    @Getter
    @ToString.Include
    private UUID id;

    @Setter
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
    private System definingSystem;

    // Reference providing/consuming components by name as provider/consumer might be external to the
    // system defining the relation
    private String providerName;
    @ToString.Include
    private String consumerName;

    /**
     * Which sources have seen this relation. Read only while the model is being imported - to decide whether a
     * relation still has a source and may therefore stay - and never served by an API.
     * <p>
     * <b>Lazy on purpose.</b> Eager, this is one {@code SELECT} per relation: an eager element collection on rows
     * that arrive through a collection is loaded per owning row, and the docs API walks every relation of every
     * system to answer for one of them. That made {@code GET /docs-api/systems/{system}} cost thousands of
     * single-row queries and seconds of latency to serve a payload that does not contain this field at all.
     * <p>
     * Every reader of it runs inside the transaction that loaded the relation - the importers under
     * {@code UpdateService.updateModel}, and the controllers that are {@code @Transactional} or
     * {@code @TransactionalReadReplica} - so nothing traverses it detached. Keep it that way: this application
     * runs with {@code spring.jpa.open-in-view=false}, so there is no session-per-request to fall back on.
     * {@code spring.jpa.properties.hibernate.default_batch_fetch_size} batches the loads that do happen, and
     * the class-level {@code onlyExplicitlyIncluded} keeps this out of every {@code toString}.
     */
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "relation_importers")
    private SortedSet<Importer> importers = new TreeSet<>();

    @NotNull
    @EqualsAndHashCode.Exclude
    @Enumerated(EnumType.STRING)
    @ToString.Include
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
