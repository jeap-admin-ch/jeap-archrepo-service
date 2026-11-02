package ch.admin.bit.jeap.archrepo.web.service;

public class UnknownSystemException extends RuntimeException {
    public UnknownSystemException(String componentName, String systemName) {
        super("Cannot create component '%s': system '%s' is not onboarded".formatted(componentName, systemName));
    }
}
