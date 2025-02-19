package ch.admin.bit.jeap.archrepo.web.rest.openapi;

public class OpenApiException extends RuntimeException {
    private OpenApiException(String message) {
        super(message);
    }

    static OpenApiException systemNotExists(String systemName) {
        String message = "System %s does not exists in ArchitectureModel".formatted(systemName);
        return new OpenApiException(message);
    }

    static OpenApiException systemComponentNotExists(String systemComponentName, String systemName) {
        String message = "SystemComponent %s does not exists in ArchitectureModel or in System %s".formatted(systemComponentName, systemName);
        return new OpenApiException(message);
    }

    static OpenApiException openApiBaseUrlIsNotSet() {
        return new OpenApiException("The 'archrepo.openapi-base-url' is not set in the configuration.");
    }

}
