package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

/**
 * A version string could not be read as a semantic version.
 */
public class SemanticVersionException extends RuntimeException {

    public SemanticVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
