package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import jakarta.servlet.http.HttpServletResponse;
import ch.admin.bit.jeap.archrepo.metamodel.ContentHash;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocsApiEtagSupportTest {

    private final DocsApiEtagSupport etagSupport = new DocsApiEtagSupport(new ObjectMapper());

    @Test
    void entityTag_isTheQuotedSha256OfTheHash() {
        assertThat(etagSupport.entityTag("41ab7c")).isEqualTo("\"sha256:41ab7c\"");
    }

    @Test
    void entityTag_ofAMissingHashIsNull() {
        assertThat(etagSupport.entityTag(null)).isNull();
    }

    @Test
    void messageVersionTag_isAQuotedSha256() {
        assertThat(etagSupport.messageVersionTag("wvs", "WvsDeclarationAcceptedEvent", "41ab7c"))
                .matches("\"sha256:[0-9a-f]{64}\"");
    }

    @Test
    void messageVersionTag_coversTheSystemAndTheMessageTypeAsStored() {
        // Both are in the body of the version resource, so a change of their spelling has to change the tag
        String tag = etagSupport.messageVersionTag("wvs", "WvsDeclarationAcceptedEvent", "41ab7c");

        assertThat(etagSupport.messageVersionTag("WVS", "WvsDeclarationAcceptedEvent", "41ab7c")).isNotEqualTo(tag);
        assertThat(etagSupport.messageVersionTag("wvs", "WvsDeclarationacceptedEvent", "41ab7c")).isNotEqualTo(tag);
        assertThat(etagSupport.messageVersionTag("wvs", "WvsDeclarationAcceptedEvent", "41ab7d")).isNotEqualTo(tag);
        assertThat(etagSupport.messageVersionTag("wvs", "WvsDeclarationAcceptedEvent", "41ab7c")).isEqualTo(tag);
    }

    @Test
    void messageVersionTag_coversTheVersionOfTheRepresentation() {
        // A change of the representation alone has to reach consumers that compare tags
        assertThat(etagSupport.messageVersionTag("wvs", "WvsDeclarationAcceptedEvent", "41ab7c"))
                .isEqualTo(etagSupport.entityTag(ContentHash.ofValues(
                        DocsApiEtagSupport.MESSAGE_VERSION_REPRESENTATION, "wvs", "WvsDeclarationAcceptedEvent",
                        "41ab7c")))
                .isNotEqualTo(etagSupport.entityTag(ContentHash.ofValues(
                        "another representation", "wvs", "WvsDeclarationAcceptedEvent", "41ab7c")));
    }

    @Test
    void messageVersionTag_ofAVersionWithoutAStoredHashIsNull() {
        assertThat(etagSupport.messageVersionTag("wvs", "WvsDeclarationAcceptedEvent", null)).isNull();
    }

    @Test
    void serialize_isStableForAnEqualBody() {
        SystemListDto one = new SystemListDto(List.of(new SystemSummaryDto("wvs", null, List.of(), null)));
        SystemListDto other = new SystemListDto(List.of(new SystemSummaryDto("wvs", null, List.of(), null)));

        assertThat(tagOf(one)).isEqualTo(tagOf(other));
    }

    @Test
    void serialize_changesWithTheBody() {
        SystemListDto one = new SystemListDto(List.of(new SystemSummaryDto("wvs", null, List.of(), null)));
        SystemListDto other = new SystemListDto(List.of(new SystemSummaryDto("other", null, List.of(), null)));

        assertThat(tagOf(one)).isNotEqualTo(tagOf(other));
    }

    @Test
    void serialize_tagsExactlyTheBytesItReturns() {
        // The whole point of returning the bytes: the tag must name the body that is actually written
        byte[] serialized =
                etagSupport.serialize(new SystemListDto(List.of(new SystemSummaryDto("wvs", null, List.of(), null))));

        assertThat(etagSupport.entityTagOf(serialized))
                .isEqualTo(etagSupport.entityTag(ContentHash.of(serialized)));
        assertThat(new String(serialized, StandardCharsets.UTF_8)).contains("\"wvs\"");
    }

    private String tagOf(Object body) {
        return etagSupport.entityTagOf(etagSupport.serialize(body));
    }

    @Test
    void isNotModified_whenTheTagMatches() {
        String entityTag = etagSupport.entityTag("41ab7c");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean notModified = etagSupport.isNotModified(webRequest(entityTag, response), entityTag);

        assertThat(notModified).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_MODIFIED);
        assertThat(response.getHeader("Cache-Control"))
                .as("a 304 repeats the cache directive a 200 would have carried")
                .isEqualTo("no-cache");
    }

    @Test
    void isNotModified_whenTheTagIsOneOfSeveral() {
        String entityTag = etagSupport.entityTag("41ab7c");

        boolean notModified = etagSupport.isNotModified(
                webRequest("\"sha256:other\", " + entityTag, new MockHttpServletResponse()), entityTag);

        assertThat(notModified).isTrue();
    }

    @Test
    void isModified_forAWildcard() {
        // Spring's conditional request handling does not treat "If-None-Match: *" as a match on a GET, so the
        // full representation is served. Recorded because it is the framework's behaviour rather than ours: the
        // doc service sends a concrete tag, and a wildcard only costs it a body it already had.
        assertThat(etagSupport.isNotModified(webRequest("*", new MockHttpServletResponse()),
                etagSupport.entityTag("41ab7c"))).isFalse();
    }

    @Test
    void isModified_whenTheTagDiffers() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean notModified = etagSupport.isNotModified(
                webRequest("\"sha256:stale\"", response), etagSupport.entityTag("41ab7c"));

        assertThat(notModified).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void isModified_withoutAnIfNoneMatchHeader() {
        assertThat(etagSupport.isNotModified(
                webRequest(null, new MockHttpServletResponse()), etagSupport.entityTag("41ab7c"))).isFalse();
    }

    private ServletWebRequest webRequest(String ifNoneMatch, MockHttpServletResponse response) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", DocsApiPaths.SYSTEMS);
        if (ifNoneMatch != null) {
            request.addHeader("If-None-Match", ifNoneMatch);
        }
        return new ServletWebRequest(request, response);
    }
}
