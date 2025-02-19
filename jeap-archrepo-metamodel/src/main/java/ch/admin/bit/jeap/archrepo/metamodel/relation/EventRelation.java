package ch.admin.bit.jeap.archrepo.metamodel.relation;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@Getter
@Entity
@DiscriminatorValue("EVENT")
public class EventRelation extends AbstractRelation {
    @NonNull
    private String eventName;

    @Override
    public String getLabel() {
        return eventName;
    }

    @Override
    public boolean isLabelLinkable() {
        return true;
    }

    @Override
    public RelationType getType() {
        return RelationType.EVENT_RELATION;
    }

    @Builder
    public EventRelation(System definingSystem, String providerName, String consumerName, Importer importer, @NonNull String eventName) {
        super(definingSystem, providerName, consumerName, importer);
        this.eventName = eventName;
    }
}
