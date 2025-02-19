package ch.admin.bit.jeap.archrepo.metamodel;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationStatus;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;

public interface Relation extends MultipleImportable {

    System getDefiningSystem();

    void setDefiningSystem(System definingSystem);

    String getProviderName();

    String getConsumerName();

    String getLabel();

    boolean isLabelLinkable();

    RelationType getType();

    RelationStatus getStatus();

}
