package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.graph.RenderedReactionGraph;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class BrowserReactionGraphRenderer {

    private static final String RESOURCE_ROOT = "/template/browser-reaction-graph/";
    private static final String GRAPH_ID_PLACEHOLDER = "@@GRAPH_ID@@";
    private static final Pattern PLACEHOLDER = Pattern.compile("@@[A-Z_]+@@");
    private static final String CSS = loadResource("graph.css");
    private static final String HTML = loadResource("graph.html");
    private static final String JAVASCRIPT = loadResource("graph.js");

    private final DocumentationGeneratorConfluenceProperties properties;

    ConfluenceReactionGraph render(RenderedReactionGraph graph) {
        return render(graph, 0, null);
    }

    ConfluenceReactionGraph render(RenderedReactionGraph graph, int occurrence) {
        String navigationKey = "Default".equals(graph.title()) ? "" : graph.title();
        return render(graph, occurrence, navigationKey);
    }

    private ConfluenceReactionGraph render(RenderedReactionGraph graph, int occurrence, String navigationKey) {
        String graphIdentity = graph.title() + "\0" + graph.dot() + "\0" + occurrence;
        String id = "reaction-graph-" + UUID.nameUUIDFromBytes(graphIdentity.getBytes(StandardCharsets.UTF_8));
        String html = "<style>\n" + replace(CSS,
                GRAPH_ID_PLACEHOLDER, id) + "</style>\n" + replace(HTML,
                GRAPH_ID_PLACEHOLDER, id,
                "@@ESCAPED_DOT@@", escapeHtml(graph.dot()),
                "@@NODE_COUNT@@", "-1",
                "@@INITIAL_HEIGHT@@", "220px",
                "@@NAVIGATION_ATTRIBUTE@@", navigationAttribute(navigationKey),
                "@@ARIA_LABEL@@", escapeHtmlAttribute("Interactive reaction graph: " + graph.title()))
                + "<script type=\"module\">\n" + replace(JAVASCRIPT,
                GRAPH_ID_PLACEHOLDER, id,
                "@@VIZ_JS_URL@@", javascriptStringLiteral(properties.getVizJsUrl()),
                "@@SVG_PAN_ZOOM_JS_URL@@", javascriptStringLiteral(properties.getSvgPanZoomJsUrl()))
                + "</script>\n";
        return new ConfluenceReactionGraph(graph.title(), wrapInCdata(html));
    }

    static String wrapInCdata(String value) {
        return "<![CDATA[" + value.replace("]]>", "]]]]><![CDATA[>") + "]]>";
    }

    private static String loadResource(String name) {
        try (InputStream stream = BrowserReactionGraphRenderer.class.getResourceAsStream(RESOURCE_ROOT + name)) {
            if (stream == null) {
                throw new IllegalStateException("Missing browser reaction graph resource: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load browser reaction graph resource: " + name, e);
        }
    }

    private static String replace(String template, String... replacements) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < replacements.length; i += 2) {
            values.put(replacements[i], replacements[i + 1]);
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder(template.length());
        while (matcher.find()) {
            String value = values.get(matcher.group());
            if (value == null) {
                throw new IllegalArgumentException("No value supplied for browser reaction graph placeholder " + matcher.group());
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        return matcher.appendTail(result).toString();
    }

    private static String navigationAttribute(String navigationKey) {
        return navigationKey == null ? "" : " data-navigation-key=\"" + escapeHtmlAttribute(navigationKey) + "\"";
    }

    private static String javascriptStringLiteral(String value) {
        return '"' + value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029")
                .replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("&", "\\u0026") + '"';
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String escapeHtmlAttribute(String value) {
        return escapeHtml(value).replace("\"", "&quot;");
    }
}
