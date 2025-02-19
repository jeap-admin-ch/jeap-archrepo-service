package ch.admin.bit.jeap.archrepo.importer.openapi;

public class OpenApiFileParsingException extends RuntimeException {

    public OpenApiFileParsingException(String message, Exception exception) {
        super(message, exception);
    }
}
