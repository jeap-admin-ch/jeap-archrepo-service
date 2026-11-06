package ch.admin.bit.jeap.archrepo.persistence;

/**
 * What the entity tag of one message type version is derived from, read without its schemas.
 *
 * @param message     the message type name <b>as it is stored</b>, which the tag covers like the response does
 * @param contentHash the stored content hash of the version, or null for a version stored before it was recorded
 */
public record MessageTypeVersionTag(String message, String contentHash) {
}
