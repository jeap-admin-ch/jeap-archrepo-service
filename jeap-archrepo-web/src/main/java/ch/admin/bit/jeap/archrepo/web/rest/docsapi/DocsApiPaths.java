package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * The routes of the docs API. Kept in one place so that no path is spelled twice - the controllers, the OpenAPI
 * group and the links the payloads carry all derive from these constants.
 */
public final class DocsApiPaths {

    /**
     * The root of the docs API. Deliberately outside {@code /api}, which is served by a filter chain using HTTP
     * basic with several public GETs; the docs API is authenticated with a bearer token and authorized with a
     * semantic role on every resource.
     */
    public static final String DOCS_API = "/docs-api";

    public static final String SYSTEMS = DOCS_API + "/systems";
    public static final String OPENAPI_SPECS = DOCS_API + "/openapi-specs";
    public static final String DATABASE_SCHEMAS = DOCS_API + "/database-schemas";
    public static final String MESSAGE_TYPES = DOCS_API + "/message-types";

    private static final String OPENAPI_CONTENT_TEMPLATE = SYSTEMS + "/{system}/components/{component}/openapi";
    private static final String DATABASE_SCHEMA_CONTENT_TEMPLATE =
            SYSTEMS + "/{system}/components/{component}/database-schema";
    static final String MESSAGE_TYPE_VERSION_TEMPLATE = MESSAGE_TYPES + "/{system}/{message}/versions/{version}";

    private DocsApiPaths() {
    }

    /**
     * @return the path of the OpenAPI spec content resource of a component, relative to the service root
     */
    public static String openApiContentPath(String systemName, String componentName) {
        return contentPath(OPENAPI_CONTENT_TEMPLATE, systemName, componentName);
    }

    /**
     * @return the path of the database schema content resource of a component, relative to the service root
     */
    public static String databaseSchemaContentPath(String systemName, String componentName) {
        return contentPath(DATABASE_SCHEMA_CONTENT_TEMPLATE, systemName, componentName);
    }

    /**
     * @return the path of one version of one message type, relative to the service root
     */
    public static String messageTypeVersionPath(String systemName, String messageName, String version) {
        return contentPath(MESSAGE_TYPE_VERSION_TEMPLATE, systemName, messageName, version);
    }

    /**
     * The path of a content resource, carrying the service's context path so that resolving it against the base
     * URL a consumer called yields the resource again. Without the context path the resolution would strip it -
     * instances run under {@code server.servlet.context-path} - which is why this is not simply
     * {@code /docs-api/...}. The host is deliberately left out: service-to-service traffic and a browser may
     * reach the arch repo under different hosts, so no single absolute URL is right for both.
     * <p>
     * System, component and message type names come from the importers rather than from a validated
     * vocabulary, so each is encoded as a path segment before it is placed into the template. Encoding after
     * expansion would not be enough: a slash is legal in a path, so it would pass through and the link would
     * name another resource.
     */
    private static String contentPath(String template, String... segments) {
        Object[] encoded = Arrays.stream(segments).map(DocsApiPaths::encodeSegment).toArray();
        return contextPath() + UriComponentsBuilder.fromPath(template)
                .build()
                .expand(encoded)
                .toUriString();
    }

    private static String encodeSegment(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    /**
     * @return the context path of the current request, or an empty string when there is none - a service at the
     * root, or a caller outside a request such as a unit test
     */
    private static String contextPath() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getContextPath();
        }
        return "";
    }
}
