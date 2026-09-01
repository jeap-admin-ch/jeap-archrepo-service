package ch.admin.bit.jeap.archrepo.persistence;

/**
 * One entry of the message type index: a message type and one of its versions, without the schemas.
 * <p>
 * The schemas of a version are the largest column pair in the model, so deciding which versions a consumer has
 * to fetch must not read them - the same reason {@link ArtifactIndexEntry} carries a hash rather than content.
 */
public interface MessageTypeVersionIndexEntry {

    String getSystem();

    /**
     * The message type name, e.g. {@code WvsDeclarationAcceptedEvent}.
     */
    String getMessage();

    /**
     * {@code EVENT} or {@code COMMAND}.
     */
    String getKind();

    String getVersion();
}
