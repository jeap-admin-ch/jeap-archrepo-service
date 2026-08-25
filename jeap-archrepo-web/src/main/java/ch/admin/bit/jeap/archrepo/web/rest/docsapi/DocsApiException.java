package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * An error of the docs API, rendered as an RFC 9457 problem document by {@link DocsApiExceptionHandler}.
 * <p>
 * The kinds are named apart on purpose: a generator has to be able to tell "no such system" from "this system has
 * no OpenAPI spec", because the first is a mistake and the second is normal.
 */
@Getter
public class DocsApiException extends RuntimeException {

    private final HttpStatus status;
    private final String problemType;
    private final String title;

    private DocsApiException(HttpStatus status, String problemType, String title, String detail) {
        super(detail);
        this.status = status;
        this.problemType = problemType;
        this.title = title;
    }

    public static DocsApiException systemNotFound(String systemName) {
        return new DocsApiException(HttpStatus.NOT_FOUND, "system-not-found", "System not found",
                "No system or alias named '" + systemName + "'");
    }

    public static DocsApiException componentNotFound(String systemName, String componentName) {
        return new DocsApiException(HttpStatus.NOT_FOUND, "component-not-found", "Component not found",
                "System '" + systemName + "' has no component named '" + componentName + "'");
    }

    public static DocsApiException openApiSpecNotFound(String componentName) {
        return new DocsApiException(HttpStatus.NOT_FOUND, "openapi-spec-not-found", "OpenAPI spec not found",
                "No OpenAPI spec has been published for the component '" + componentName + "'");
    }

    public static DocsApiException databaseSchemaNotFound(String componentName) {
        return new DocsApiException(HttpStatus.NOT_FOUND, "database-schema-not-found", "Database schema not found",
                "No database schema has been published for the component '" + componentName + "'");
    }

}
