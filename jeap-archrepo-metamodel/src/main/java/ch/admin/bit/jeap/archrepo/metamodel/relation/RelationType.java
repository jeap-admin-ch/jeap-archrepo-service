package ch.admin.bit.jeap.archrepo.metamodel.relation;

import lombok.Getter;

@Getter
public enum RelationType {
    REST_API_RELATION(RestApiRelation.class, "REST API Relation"),
    EVENT_RELATION(EventRelation.class, "Event Relation"),
    COMMAND_RELATION(CommandRelation.class, "Command Relation");

    private final Class<?> type;
    private final String label;

    RelationType(Class<?> type, String label) {
        this.type = type;
        this.label = label;
    }
}
