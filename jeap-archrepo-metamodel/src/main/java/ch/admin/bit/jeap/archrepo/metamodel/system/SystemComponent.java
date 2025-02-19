package ch.admin.bit.jeap.archrepo.metamodel.system;

import ch.admin.bit.jeap.archrepo.metamodel.Importable;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Getter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class SystemComponent implements Importable {

    // SystemComponent is kept 2 weeks in model before deletion
    private static final int NB_OF_DAYS_TO_KEEP_SYSTEM_COMPONENT = 14;

    @Id
    @NotNull
    private UUID id;

    @NonNull
    String name;

    String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @Setter
    Team ownedBy;

    public abstract SystemComponentType getType();

    @Enumerated(EnumType.STRING)
    Importer importer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_id")
    @Setter
    System parent;

    @NotNull
    ZonedDateTime createdAt;

    @EqualsAndHashCode.Exclude
    ZonedDateTime lastSeen;

    protected SystemComponent(String name) {
        this.id = UUID.randomUUID();
        this.createdAt = ZonedDateTime.now();
        this.name = name;
    }

    public void setLastSeenFromDate(ZonedDateTime zonedDateTime) {
        lastSeen = zonedDateTime;
    }

    public void setLastSeenFromNow() {
        this.setLastSeenFromDate(ZonedDateTime.now());
    }

    public boolean isObsolete() {
        if (lastSeen == null) {
            // System Component without lastSeen date is always obsolete
            return true;
        }
        return lastSeen.isBefore(ZonedDateTime.now().minusDays(NB_OF_DAYS_TO_KEEP_SYSTEM_COMPONENT));
    }

    @Override
    public void setImporter(Importer importer) {
        this.importer = importer;
    }
}
