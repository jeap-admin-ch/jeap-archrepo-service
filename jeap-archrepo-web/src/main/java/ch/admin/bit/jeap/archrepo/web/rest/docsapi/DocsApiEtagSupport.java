package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.ContentHash;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import tools.jackson.databind.ObjectMapper;

/**
 * Entity tags and conditional requests for the docs API.
 * <p>
 * A tag is always {@code "sha256:<hex>"} over the bytes the resource is about. For the two content resources
 * those are the stored bytes of the artifact, which is what makes the tag of an index entry byte-identical to the
 * tag of the resource it points at: a consumer can compare without a request, and send the stored value as
 * {@code If-None-Match} when it does fetch.
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
