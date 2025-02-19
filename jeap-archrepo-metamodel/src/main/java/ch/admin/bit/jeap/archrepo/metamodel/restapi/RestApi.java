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
@ToString
@Entity
@Getter
public class RestApi extends MutableDomainEntity implements MultipleImportable {

    @Id
    @NotNull
    private UUID id;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
    private System definingSystem;

    @NotNull
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @ToString.Exclude
    private SystemComponent provider;

    @NotNull
    private String method;

    @NotNull
    private String path;

    @EqualsAndHashCode.Exclude
    @ElementCollection(fetch = FetchType.EAGER)
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
        String replaced = path.replaceAll("\\{.*?}", "{}");
        if (!replaced.endsWith("/") || replaced.length() == 1) {
            return replaced;
        }
        return replaced.substring(0, replaced.length() - 1);
    }
}
