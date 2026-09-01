package ch.admin.bit.jeap.archrepo.persistence;

/**
 * One version of one message type, with its schemas: everything stored about it, read as a row rather than
 * through the architecture model.
 * <p>
 * A constructor projection rather than an interface one - ten columns read more clearly as a record, and the
 * message type name is projected next to them so that a consumer answering a request is not left echoing the
 * spelling the caller used.
 *
 * @param message            the message type name <b>as it is stored</b>
 * @param keySchemaName      the key schema's file name, or null - only the value schema is mandatory
 * @param keySchemaResolved  the key schema as {@code SchemaImportResolver} rendered it at import time, or null
 * @param valueSchemaResolved the value schema, rendered the same way
 */
public record MessageTypeVersionDetail(
        String message,
        String version,
        String compatibilityMode,
        String compatibleVersion,
        String keySchemaName,
        String keySchemaUrl,
        String keySchemaResolved,
        String valueSchemaName,
        String valueSchemaUrl,
        String valueSchemaResolved) {
}
