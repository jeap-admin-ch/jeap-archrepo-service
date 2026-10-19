package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.graph.RenderedReactionGraph;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserReactionGraphRendererTest {

    @Test
    void rendersSearchableSvgWithAdaptivePanZoomControls() {
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
                .contains("svg.querySelectorAll(\"g.node\").length")
                .contains("mouseWheelZoomEnabled: true")
                .contains("controlIconsEnabled: false")
                .contains("const idMap = new Map()")
                .contains("targetSvg.querySelectorAll(\"[id]\")")
                .contains("data-node-count=\"-1\"")
                .contains("role=\"region\" aria-label=\"Interactive reaction graph: Orders\"")
                .contains("height:220px")
                .contains("data-action=\"actual-size\"")
                .contains("data-action=\"fit\"")
                .contains("2 / fitRealZoom")
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
                .doesNotContain("a</textarea>")
                .doesNotContain("@@");
    }

    @Test
    void addsInteractiveMinimapAndReadableInitialViewForLargeGraphs() {
        ConfluenceReactionGraph graph = new BrowserReactionGraphRenderer(new DocumentationGeneratorConfluenceProperties()).render(
                new RenderedReactionGraph("priority&\"high\"", "digraph G { a -> b }"), 0);

        assertThat(graph.cdata())
                .contains("data-node-count=\"-1\"")
                .contains("data-navigation-key=\"priority&amp;&quot;high&quot;\"")
                .contains("nodeCount > minimapThreshold")
                .contains("class=\"reaction-graph-map-toggle\" hidden")
                .contains("aria-expanded=\"false\"")
                .contains("title=\"Show graph overview map\"")
                .contains("M3 6l6-3 6 3 6-3v15l-6 3-6-3-6 3z")
                .contains("class=\"reaction-graph-minimap-close\"")
                .contains("aria-label=\"Close graph overview map\"")
                .contains("M6 6l12 12M18 6L6 18")
                .contains("const ensureMinimap = () =>")
                .contains("minimapSvg = svg.cloneNode(true)")
                .contains("minimapViewport.removeAttribute(\"transform\")")
                .contains("minimap.prepend(minimapSvg)")
                .contains("minimap.hidden = !expanded")
                .contains("mapToggle.hidden = expanded")
                .contains("if (expanded) minimapClose.focus()")
                .contains("minimapClose.addEventListener(\"click\"")
                .contains("event.key === \"Escape\"")
                .contains("window.__jeapArchRepoActiveReactionGraphMap === mapController")
                .contains("event.key === \"ArrowRight\"")
                .contains("event.key === \"Home\"")
                .contains("reaction-graph-minimap-viewport")
                .contains("panZoom.setOnUpdatedCTM(updateMinimap)")
                .contains("0.8 / fitRealZoom")
                .contains("const minimumRelativeZoom = Math.min(1, 1 / fitRealZoom)")
                .contains("viewportRect.addEventListener(\"pointermove\"")
                .contains("touch-action: none")
                .contains("if (!canPreserveView)")
                .doesNotContain("minimapSvgMarkup")
                .doesNotContain("svg.outerHTML")
                .doesNotContain("vis-network");
    }

    @Test
    void focusesDeepLinkedNodeInMatchingGraphVariant() {
        ConfluenceReactionGraph graph = new BrowserReactionGraphRenderer(new DocumentationGeneratorConfluenceProperties()).render(
                new RenderedReactionGraph("priority", "digraph G { a -> b }"), 0);

        assertThat(graph.cdata())
                .contains("const navigationPrefix = \"#archrepo-graph?\"")
                .contains("typeof hash !== \"string\" || !hash.startsWith(navigationPrefix)")
                .contains("new URLSearchParams(hash.substring(navigationPrefix.length))")
                .contains("graphContainer.dataset.navigationKey !== request.variant")
                .contains("node.querySelector(\"title\")?.textContent === request.node")
                .contains("graphContainer.scrollIntoView({ behavior: \"instant\", block: \"center\" })")
                .contains("panZoom.zoom(1 / currentFitScale)")
                .contains("panZoom.panBy({")
                .contains("reaction-graph-target")
                .contains("window.setTimeout(clearFocusedNode, 5000)")
                .contains("navigationWindow?.addEventListener(\"hashchange\"")
                .contains("let navigationWindow = window")
                .contains("url.searchParams.get(\"archrepoGraphNode\")")
                .contains("url.searchParams.has(\"archrepoGraphVariant\")")
                .contains("document.referrer")
                .contains("sessionStorage.setItem(pendingNavigationKey")
                .contains("const age = Date.now() - Number(pending?.createdAt)")
                .contains("!Number.isFinite(age) || age < 0 || age > 60000")
                .contains("pageId: targetUrl.searchParams.get(\"pageId\")")
                .contains("if (pending.pageId && currentPageIds.length > 0 && !currentPageIds.includes(pending.pageId))")
                .contains("url.searchParams.delete(\"archrepoGraphNode\")")
                .contains("targetUrl.origin === currentUrl.origin && targetPageId === currentPageId")
                .contains("const resolveDeepLinkTarget = hash =>")
                .contains("if (unmodifiedPrimaryClick && targetUrl.hash.startsWith(navigationPrefix))")
                .contains("rememberNavigation(targetUrl)")
                .contains("clearPendingNavigation(hash)")
                .contains("window.frameElement.scrollIntoView({ behavior: \"instant\", block: \"center\" })")
                .contains("const unmodifiedPrimaryClick = event.button === 0")
                .contains("isSamePage(targetUrl, currentUrl)")
                .contains("resolveDeepLinkTarget(targetUrl.hash)")
                .contains("navigationWindow.history.replaceState(navigationWindow.history.state, \"\", currentUrl)")
                .contains("const scheduleFrame = callback =>")
                .contains("scheduleFrame(() => applyDeepLink())")
                .contains("navigationStatus.textContent = \"Focused graph node \" + request.node")
                .contains("targetLink.focus({ preventScroll: true })")
                .contains("navigationWindow?.removeEventListener(\"hashchange\"")
                .contains("resizeObserver?.disconnect()")
                .contains("themeObserver.disconnect()")
                .contains("window.addEventListener(\"pagehide\", handlePageHide)")
                .contains("if (!event.persisted) teardown()")
                .contains("window.cancelAnimationFrame(frameId)")
                .contains("panZoom.destroy()")
                .doesNotContain("behavior: \"smooth\"");
    }

    @Test
    void addsFocusedCaseInsensitiveSearchWithWrappingNavigationAndReset() {
        ConfluenceReactionGraph graph = new BrowserReactionGraphRenderer(new DocumentationGeneratorConfluenceProperties()).render(
                new RenderedReactionGraph("Search", "digraph G { Order -> Payment }"));

        assertThat(graph.cdata())
                .contains("class=\"reaction-graph-search\" role=\"search\" hidden")
                .contains("aria-label=\"Search nodes in graph\"")
                .contains("class=\"reaction-graph-search-count\"")
                .contains(">0/0</span>")
                .contains("data-search-action=\"previous\"")
                .contains("data-search-action=\"next\"")
                .contains("data-search-action=\"close\"")
                .contains("searchNavigationButtons.forEach(button => button.disabled = searchMatches.length === 0)")
                .contains(".join(\" \")\n        .toLowerCase()")
                .contains("text.includes(query)")
                .contains("currentSearchMatch = searchMatches.length > 0 ? 0 : -1")
                .contains("(currentSearchMatch + direction + searchMatches.length) % searchMatches.length")
                .doesNotContain("searchNavigationStarted")
                .contains("navigateSearch(event.shiftKey ? -1 : 1)")
                .contains("(event.ctrlKey || event.metaKey)")
                .contains("event.key.toLowerCase() === \"f\"")
                .contains("window.__jeapArchRepoActiveReactionGraphSearch === searchController")
                .contains("if (activeSearch && activeSearch !== searchController) activeSearch.reset()")
                .contains("minimapSearchNode(index)?.classList.add(\"reaction-graph-search-match\")")
                .contains("minimapSearchNode(currentIndex)?.classList.add(\"reaction-graph-search-current\")")
                .contains("--graph-search-match: #946f00")
                .contains("--graph-search-current: #0c66e4")
                .contains("--graph-search-match: #f5cd47")
                .contains("--graph-search-current: #85b8ff")
                .contains("if (action === \"close\") resetSearch(true)")
                .contains("if (!search.hidden && !search.contains(event.target)) resetSearch()")
                .contains("graphContainer.addEventListener(\"focusout\", handleGraphFocusOut)")
                .contains("graphContainer.removeEventListener(\"focusout\", handleGraphFocusOut)")
                .contains("graphContainer.focus({ preventScroll: true })")
                .contains("if (!graphContainer.isConnected)")
                .contains("break reactionGraphInitialization")
                .contains("@media (max-width: 480px)")
                .contains("reaction-graph-search-open .reaction-graph-controls > button[data-action]")
                .contains("window.addEventListener(\"blur\", handleWindowBlur)")
                .contains("window.removeEventListener(\"blur\", handleWindowBlur)")
                .contains("document.removeEventListener(\"pointerdown\", handleDocumentPointerDown)");
    }

    @Test
    void derivesNodeCountFromRenderedSvg() {
        RenderedReactionGraph renderedGraph = new RenderedReactionGraph("Legacy", "digraph G { a -> b }");

        ConfluenceReactionGraph graph = new BrowserReactionGraphRenderer(new DocumentationGeneratorConfluenceProperties())
                .render(renderedGraph);

        assertThat(graph.cdata())
                .contains("data-node-count=\"-1\"")
                .contains("nodeCount = Math.max(Number.isFinite(nodeCount) ? nodeCount : 0")
                .contains("graphContainer.style.height = \"clamp(480px, 70vh, 760px)\"");
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

    @Test
    void doesNotReplacePlaceholderLikeTextInsideDotSource() {
        ConfluenceReactionGraph graph = new BrowserReactionGraphRenderer(new DocumentationGeneratorConfluenceProperties())
                .render(new RenderedReactionGraph("Tokens", "digraph G { label=\"@@NODE_COUNT@@\" }"));

        assertThat(graph.cdata()).contains("label=\"@@NODE_COUNT@@\"");
    }
}
