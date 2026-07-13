package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.graph.RenderedReactionGraph;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserReactionGraphRendererTest {

    @Test
    void rendersSearchableSvgWithPanZoomAndInitialFit() {
        DocumentationGeneratorConfluenceProperties properties = new DocumentationGeneratorConfluenceProperties();
        properties.setVizJsUrl("https://example.test/\"quoted\"\n</script>/viz.js");
        ConfluenceReactionGraph graph = new BrowserReactionGraphRenderer(properties).render(
                new RenderedReactionGraph("Orders", "digraph G { \"a</textarea>]]>\" -> b }"));

        assertThat(graph.cdata())
                .startsWith("<![CDATA[")
                .endsWith("]]>")
                .contains("import(\"https://example.test/\\\"quoted\\\"\\n\\u003c/script\\u003e/viz.js\")")
                .contains("svg-pan-zoom@3.6.2")
                .contains("window.__jeapArchRepoReactionGraphRuntime")
                .contains("renderSVGElement(dot)")
                .contains("mouseWheelZoomEnabled: true")
                .contains("const idMap = new Map()")
                .contains("svg.querySelectorAll(\"[id]\")")
                .contains("panZoom.fit()")
                .contains("panZoom.center()")
                .contains("ResizeObserver")
                .contains("var(--ds-text, #172b4d)")
                .contains("reaction-graph-dark")
                .contains("data-color-mode")
                .contains("normalizedMode === \"dark\"")
                .contains("prefers-color-scheme: dark")
                .contains("svg > g.graph > polygon { fill: transparent; }")
                .contains("a&lt;/textarea&gt;")
                .doesNotContain("a</textarea>");
    }

    @Test
    void assignsDifferentIdsToIdenticalGraphsOnTheSamePage() {
        BrowserReactionGraphRenderer renderer = new BrowserReactionGraphRenderer(new DocumentationGeneratorConfluenceProperties());
        RenderedReactionGraph graph = new RenderedReactionGraph("Default", "digraph G { a -> b }");

        ConfluenceReactionGraph first = renderer.render(graph, 0);
        ConfluenceReactionGraph second = renderer.render(graph, 1);

        assertThat(first.cdata()).isNotEqualTo(second.cdata());
    }

    @Test
    void safelySplitsCdataTerminator() {
        assertThat(BrowserReactionGraphRenderer.wrapInCdata("before]]>after"))
                .isEqualTo("<![CDATA[before]]]]><![CDATA[>after]]>");
    }
}
