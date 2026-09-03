package ch.admin.bit.jeap.archrepo.metamodel.restapi;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.MultipleImportable;
import ch.admin.bit.jeap.archrepo.metamodel.MutableDomainEntity;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.*;

@EqualsAndHashCode(callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Getter
public class RestApi extends MutableDomainEntity implements MultipleImportable {

    @Id
    @NotNull
    @ToString.Include
    private UUID id;

    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
    private System definingSystem;

    @NotNull
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    private SystemComponent provider;

    @NotNull
    @ToString.Include
    private String method;

    @NotNull
    @ToString.Include
    private String path;

    /**
     * Which sources have seen this REST API. See the same field on
     * {@link ch.admin.bit.jeap.archrepo.metamodel.relation.AbstractRelation} for why it is lazy; the same
     * per-owning-row {@code SELECT} applies here.
     * <p>
     * Not in {@code toString}: this class is logged as a whole object by several importers, and every one of
     * those log lines would otherwise load this collection from the database. The class-level
     * {@code onlyExplicitlyIncluded} is what keeps it out, so there is nothing to remember when a field is
     * added here.
     */
    @EqualsAndHashCode.Exclude
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "rest_api_importers")
    @Enumerated(EnumType.STRING)
    private SortedSet<Importer> importers = new TreeSet<>();

    protected RestApi() {
        super();
    }

    @Builder
    public RestApi(@NonNull SystemComponent provider, @NonNull String method, @NonNull String path, Importer importer) {
        this.id = UUID.randomUUID();
        this.definingSystem = provider.getParent();
        this.provider = provider;
        this.method = method.toUpperCase();
        this.path = path;
        addImporter(importer);
    }

    public void updatePath(String path) {
        this.path = path;
    }

    @Override
    public void addImporter(Importer importer) {
        if (importer != null) {
            this.importers.add(importer);
        }
    }

    public void removeImporter(Importer importer) {
        if (importer != null) {
            this.importers.remove(importer);
        }
    }

    @Override
    public Set<Importer> getImporters() {
        return Collections.unmodifiableSet(this.importers);
    }

    public boolean pathMatches(String path) {
        return pathWithoutVariableNames(this.path).equals(pathWithoutVariableNames(path));
    }

    private static String pathWithoutVariableNames(String path) {
        String replaced = path.replaceAll("\\{[^}]*+}", "{}");
        if (!replaced.endsWith("/") || replaced.length() == 1) {
            return replaced;
        }
        return replaced.substring(0, replaced.length() - 1);
    }
}
