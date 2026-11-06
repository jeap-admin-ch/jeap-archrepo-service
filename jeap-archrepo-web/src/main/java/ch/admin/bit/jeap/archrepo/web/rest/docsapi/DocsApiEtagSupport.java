package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.ContentHash;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import tools.jackson.databind.ObjectMapper;

/**
 * Entity tags and conditional requests for the docs API.
 * <p>
 * A tag is always {@code "sha256:<hex>"}, and it is computed from what is <b>stored</b> wherever an index lists
 * the resource, so that the tag of an index entry is byte-identical to the tag of the resource it points at: a
 * consumer can compare without a request, and send the stored value as {@code If-None-Match} when it does fetch.
 * <ul>
 *     <li>The two content resources: the stored hash of the artifact's bytes - see {@link #entityTag(String)}.</li>
 *     <li>A message type version: derived from the system, the message type and the stored hash of the
 *     version's fields - see {@link #messageVersionTag(String, String, String)}. A version stored before that
 *     hash existed is tagged over its serialized body instead, and its index entry carries no tag.</li>
 *     <li>Every other resource: the serialized body - see {@link #respond(WebRequest, Object)}.</li>
 * </ul>
 * <p>
 * This holds only because every resource serves exactly one representation. A second representation would need
 * one tag per representation, and the index could no longer carry the content resource's tag.
 */
@Component
@RequiredArgsConstructor
public class DocsApiEtagSupport {

    private final ObjectMapper objectMapper;

    /**
     * @param contentHash the stored hash of an artifact, may be null
     * @return the entity tag for that hash, or null if there is none
     */
    public String entityTag(String contentHash) {
        return contentHash == null ? null : "\"sha256:" + contentHash + "\"";
    }

    /**
     * The entity tag of one message type version, as both the message type index and the version resource carry
     * it.
     * <p>
     * The version resource serves the system and the message type next to the stored fields, in their stored
     * spelling, and those two are not part of the version's stored hash - so they are folded in here. A system
     * renamed in case, for example, answers with a different body and therefore has to carry a different tag.
     *
     * @param system      the system as it is stored
     * @param message     the message type name as it is stored
     * @param contentHash the stored hash of the version, may be null
     * @return the entity tag, or null when the version has no stored hash
     */
    public String messageVersionTag(String system, String message, String contentHash) {
        return contentHash == null ? null : entityTag(ContentHash.ofValues(system, message, contentHash));
    }

    /**
     * Serializes a response body to the bytes that are written. The caller keeps them and tags them with
     * {@link #entityTagOf(byte[])}: the alternative is to serialize once for the tag and let the message
     * converter serialize the same object again for the body, which doubles the cost of the largest payloads of
     * this API and throws the first result away on a {@code 304}.
     */
    public byte[] serialize(Object body) {
        return objectMapper.writeValueAsBytes(body);
    }

    /**
     * @return the entity tag of an already serialized body, so the tag names exactly the bytes on the wire
     */
    public String entityTagOf(byte[] serializedBody) {
        return entityTag(ContentHash.of(serializedBody));
    }

    /**
     * Answers with the body and its entity tag, or with {@code 304} when the caller already has it.
     * <p>
     * Every resource of this API answers this way, and it lives here rather than in each controller because a
     * slip in the sequence - tagging bytes other than the ones written, or forgetting the directive on the
     * {@code 304} - would break conditional requests on one resource only, quietly.
     *
     * @return the response, or null when the prepared {@code 304} is to be sent
     */
    public ResponseEntity<byte[]> respond(WebRequest request, Object body) {
        byte[] serialized = serialize(body);
        String entityTag = entityTagOf(serialized);
        if (isNotModified(request, entityTag)) {
            return null;
        }
        return ResponseEntity.ok()
                .eTag(entityTag)
                .cacheControl(CacheControl.noCache())
                .body(serialized);
    }

    /**
     * Answers with the body under an entity tag the caller derived from stored data, which it has already
     * evaluated against {@code If-None-Match} with {@link #isNotModified(WebRequest, String)} - before reading
     * what the body is made of.
     */
    public ResponseEntity<byte[]> respondTagged(String entityTag, Object body) {
        return ResponseEntity.ok()
                .eTag(entityTag)
                .cacheControl(CacheControl.noCache())
                .body(serialize(body));
    }

    /**
     * Evaluates {@code If-None-Match} and, when it matches, prepares the {@code 304} response.
     *
     * @return true when the caller should return null so that the prepared {@code 304} is sent
     */
    public boolean isNotModified(WebRequest request, String entityTag) {
        boolean notModified = request.checkNotModified(entityTag);
        if (notModified) {
            // The 304 is written by Spring, bypassing the ResponseEntity the 200 path builds - so the cache
            // directive has to be repeated here, or a consumer would see it on the 200 and not on the 304.
            setCacheControl(request);
        }
        return notModified;
    }

    private void setCacheControl(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletResponse response = servletWebRequest.getResponse();
            if (response != null) {
                response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue());
            }
        }
    }
}
