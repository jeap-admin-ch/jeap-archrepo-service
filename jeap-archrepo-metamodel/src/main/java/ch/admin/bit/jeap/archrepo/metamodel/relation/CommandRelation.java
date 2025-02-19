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
@DiscriminatorValue("COMMAND")
public class CommandRelation extends AbstractRelation {

    @NonNull
    private String commandName;

    @Override
    public String getLabel() {
        return commandName;
    }

    @Override
    public boolean isLabelLinkable() {
        return true;
    }

    @Override
    public RelationType getType() {
        return RelationType.COMMAND_RELATION;
    }

    @Builder
    public CommandRelation(System definingSystem, String providerName, String consumerName, Importer importer, @NonNull String commandName) {
        super(definingSystem, providerName, consumerName, importer);
        this.commandName = commandName;
    }
}
