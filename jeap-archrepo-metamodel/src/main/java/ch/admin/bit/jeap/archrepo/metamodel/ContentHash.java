package ch.admin.bit.jeap.archrepo.metamodel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 over the stored bytes of an artifact, used as the entity tag of the artifacts the docs API serves.
 * <p>
 * The hash is stored with the artifact rather than computed per request: an index over all artifacts would
 * otherwise have to read every blob, and a conditional request would have to read the blob it is about to not
 * return.
 */
public final class ContentHash {

    private ContentHash() {
    }

    /**
     * @param content the stored bytes, may be null
     * @return the lowercase hex encoded SHA-256 of the content, or null if the content is null
     */
    public static String of(byte[] content) {
        if (content == null) {
            return null;
        }
        return HexFormat.of().formatHex(sha256(content));
    }

    /**
     * SHA-256 over a sequence of values rather than over stored bytes - for what is stored as several columns,
     * such as a message type version, and still has to be tagged as one thing.
     * <p>
     * The encoding is unambiguous: every value is prefixed with its length, and a null is written as a marker no
     * length can produce. So {@code ("ab", "c")} and {@code ("a", "bc")} differ, and so do a null and an empty
     * string - a key schema that is absent is not one that is blank.
     *
     * @param values the values in a fixed order, each may be null
     * @return the lowercase hex encoded SHA-256 of the encoded values
     */
    public static String ofValues(String... values) {
        StringBuilder encoded = new StringBuilder();
        for (String value : values) {
            if (value == null) {
                encoded.append("-;");
            } else {
                encoded.append(value.length()).append(':').append(value).append(';');
            }
        }
        return of(encoded.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] sha256(byte[] content) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(content);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required to be present on every Java platform
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
