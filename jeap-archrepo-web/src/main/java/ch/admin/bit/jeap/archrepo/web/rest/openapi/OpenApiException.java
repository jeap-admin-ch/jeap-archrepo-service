package ch.admin.bit.jeap.archrepo.web.rest.openapi;

public class OpenApiException extends RuntimeException {
    private OpenApiException(String message) {
        super(message);
    }

    static OpenApiException systemComponentNotExists(String systemComponentName) {
        String message = "SystemComponent %s does not exists in architecture model".formatted(systemComponentName);
        return new OpenApiException(message);
    }

    static OpenApiException openApiBaseUrlIsNotSet() {
        return new OpenApiException("The 'archrepo.openapi-base-url' is not set in the configuration.");
    }

}
