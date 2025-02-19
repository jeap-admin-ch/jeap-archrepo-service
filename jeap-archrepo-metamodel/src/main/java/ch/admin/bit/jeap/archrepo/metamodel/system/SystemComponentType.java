package ch.admin.bit.jeap.archrepo.metamodel.system;

import lombok.Getter;

@Getter
public enum SystemComponentType {
    BACKEND_SERVICE(BackendService.class, "Backend Service"),
    FRONTEND(Frontend.class, "Frontend"),
    MOBILE_APP(MobileApp.class, "Mobile App"),
    SELF_CONTAINED_SYSTEM(SelfContainedSystem.class, "Self-Contained System"),
    UNKNOWN(UnknownSystemComponent.class, "Unknown System");

    private final Class<?> type;
    private final String label;

    SystemComponentType(Class<?> type, String label) {
        this.type = type;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
