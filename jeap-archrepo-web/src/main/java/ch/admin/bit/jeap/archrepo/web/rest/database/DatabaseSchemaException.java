package ch.admin.bit.jeap.archrepo.web.rest.database;


import lombok.Getter;
import org.springframework.http.HttpStatus;

public class DatabaseSchemaException extends RuntimeException {

    @Getter
    private final HttpStatus responseStatus;

    private DatabaseSchemaException(String message, HttpStatus responseStatus) {
        super(message);
        this.responseStatus = responseStatus;
    }

    private DatabaseSchemaException(String message, HttpStatus responseStatus, Exception e) {
        super(message, e);
        this.responseStatus = responseStatus;
    }

    static DatabaseSchemaException systemDoesNotExist(String systemName) {
        String message = "The system '%s' does not exist in the architecture model.".formatted(systemName);
        return new DatabaseSchemaException(message, HttpStatus.BAD_REQUEST);
    }

    static DatabaseSchemaException systemComponentDoesNotExist(String systemComponentName) {
        String message = "The system component '%s' does not exist in the architecture model.".
                formatted(systemComponentName);
        return new DatabaseSchemaException(message, HttpStatus.BAD_REQUEST);
    }

    @SuppressWarnings("SameParameterValue")
    static DatabaseSchemaException unexpectedError(CreateOrUpdateDbSchemaDto schemaDto, String message) {
        return new DatabaseSchemaException(unexpectedErrorMessage(schemaDto, message), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    static DatabaseSchemaException unexpectedError(CreateOrUpdateDbSchemaDto schemaDto, Exception e) {
        return new DatabaseSchemaException(unexpectedErrorMessage(schemaDto, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    static DatabaseSchemaException unexpectedError(String systemComponent, Exception e) {
        return new DatabaseSchemaException(unexpectedErrorMessage(systemComponent, e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    static DatabaseSchemaException schemaSerializationError(CreateOrUpdateDbSchemaDto schemaDto, Exception e) {
        String message = "Unable to serialize the database schema for the component '%s': %s".
                formatted(schemaDto.getSystemComponentName(), e.getMessage());
        return new DatabaseSchemaException(message, HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    static DatabaseSchemaException schemaDeserializationError(String system, String systemComponent, Exception e) {
        String message = "Unable to deserialize the database schema for the component '%s' and the system '%s': %s".
                formatted(systemComponent, system, e.getMessage());
        return new DatabaseSchemaException(message, HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    private static String unexpectedErrorMessage(CreateOrUpdateDbSchemaDto schemaDto, String message) {
        return unexpectedErrorMessage(schemaDto.getSystemComponentName(), message);
    }

    private static String unexpectedErrorMessage(String systemComponent, String message) {
        return "An unexpected error happened while processing the database schema for the component '%s': %s."
                .formatted(systemComponent, message);
    }

}
