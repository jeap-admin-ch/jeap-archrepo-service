package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.graph.RenderedReactionGraph;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class BrowserReactionGraphRenderer {

    private final DocumentationGeneratorConfluenceProperties properties;

    ConfluenceReactionGraph render(RenderedReactionGraph graph) {
        return render(graph, 0);
    }

    ConfluenceReactionGraph render(RenderedReactionGraph graph, int occurrence) {
        String graphIdentity = graph.title() + "\0" + graph.dot() + "\0" + occurrence;
        String id = "reaction-graph-" + UUID.nameUUIDFromBytes(graphIdentity.getBytes(StandardCharsets.UTF_8));
        String escapedDot = escapeHtml(graph.dot());
        String vizJsUrl = javascriptStringLiteral(properties.getVizJsUrl());
        String svgPanZoomJsUrl = javascriptStringLiteral(properties.getSvgPanZoomJsUrl());
        String html = """
                <style>
                  #%1$s {
                    --graph-text: var(--ds-text, #172b4d);
                    --graph-border: var(--ds-border, #8590a2);
                    --graph-edge: var(--ds-text-subtle, #5e6c84);
                    --graph-highlight: var(--ds-background-information, #deebff);
                    --graph-surface: var(--ds-surface, #ffffff);
                    color: var(--graph-text);
                    background: var(--graph-surface);
                    border-color: var(--graph-border) !important;
                  }
                  #%1$s.reaction-graph-dark {
                    --graph-text: var(--ds-text, #b6c2cf);
                    --graph-border: var(--ds-border, #738496);
                    --graph-edge: var(--ds-text-subtle, #9fadbc);
                    --graph-highlight: var(--ds-background-information, #0c4477);
                    --graph-surface: var(--ds-surface, #1d2125);
                  }
                  #%1$s svg > g.graph > polygon { fill: transparent; }
                  #%1$s svg text { fill: var(--graph-text); }
                  #%1$s svg .node ellipse,
                  #%1$s svg .node polygon,
                  #%1$s svg .cluster polygon { stroke: var(--graph-border); }
                  #%1$s svg .edge path { stroke: var(--graph-edge); }
                  #%1$s svg .edge polygon { fill: var(--graph-edge); stroke: var(--graph-edge); }
                  #%1$s svg [fill="lightblue"] { fill: var(--graph-highlight); }
                  #%1$s svg a:hover text { text-decoration: underline; }
                  @media (prefers-color-scheme: dark) {
                    #%1$s:not(.reaction-graph-light) {
                      --graph-text: var(--ds-text, #b6c2cf);
                      --graph-border: var(--ds-border, #738496);
                      --graph-edge: var(--ds-text-subtle, #9fadbc);
                      --graph-highlight: var(--ds-background-information, #0c4477);
                      --graph-surface: var(--ds-surface, #1d2125);
                    }
                  }
                </style>
                <div id="%1$s" class="reaction-graph" style="width:100%%;height:600px;border:1px solid;overflow:hidden;">
                  <textarea id="%1$s-source" hidden>%2$s</textarea>
                  <div id="%1$s-content" style="width:100%%;height:100%%;">Rendering reaction graph...</div>
                </div>
                <script type="module">
                  const graphContainer = document.getElementById("%1$s");
                  const container = document.getElementById("%1$s-content");
                  const dot = document.getElementById("%1$s-source").value;
                  const colorScheme = window.matchMedia("(prefers-color-scheme: dark)");
                  let themeRoot = document.documentElement;
                  try {
                    themeRoot = window.parent.document.documentElement;
                  } catch (ignored) {
                    // Cross-origin macro frames fall back to their own document and browser preference.
                  }
                  const applyTheme = () => {
                    const mode = themeRoot.getAttribute("data-color-mode") || themeRoot.getAttribute("data-theme");
                    const normalizedMode = mode?.toLowerCase();
                    const explicitDark = normalizedMode === "dark" || normalizedMode?.startsWith("dark:");
                    const explicitLight = normalizedMode === "light" || normalizedMode?.startsWith("light:");
                    const hasExplicitMode = explicitDark || explicitLight;
                    const dark = explicitDark || (!hasExplicitMode && colorScheme.matches);
                    graphContainer.classList.toggle("reaction-graph-dark", dark);
                    graphContainer.classList.toggle("reaction-graph-light", hasExplicitMode && !dark);
                  };
                  applyTheme();
                  new MutationObserver(applyTheme).observe(themeRoot, {
                    attributes: true,
                    attributeFilter: ["data-color-mode", "data-theme"]
                  });
                  colorScheme.addEventListener("change", applyTheme);
                  try {
                    const runtime = window.__jeapArchRepoReactionGraphRuntime ??= Promise.all([
                      import(%3$s),
                      import(%4$s)
                    ]).then(async ([vizModule, panZoomModule]) => ({
                      viz: await vizModule.instance(),
                      svgPanZoom: panZoomModule.default
                    }));
                    const { viz, svgPanZoom } = await runtime;
                    const svg = viz.renderSVGElement(dot);
                    svg.removeAttribute("width");
                    svg.removeAttribute("height");
                    svg.style.width = "100%%";
                    svg.style.height = "100%%";
                    container.replaceChildren(svg);
                    const panZoom = svgPanZoom(svg, {
                      panEnabled: true,
                      zoomEnabled: true,
                      mouseWheelZoomEnabled: true,
                      controlIconsEnabled: true,
                      fit: true,
                      center: true
                    });
                    const idMap = new Map();
                    for (const element of [svg, ...svg.querySelectorAll("[id]")]) {
                      if (element.id) {
                        const namespacedId = "%1$s-" + element.id;
                        idMap.set(element.id, namespacedId);
                        element.id = namespacedId;
                      }
                    }
                    for (const element of svg.querySelectorAll("*")) {
                      for (const attributeName of ["href", "xlink:href"]) {
                        const reference = element.getAttribute(attributeName);
                        if (reference?.startsWith("#") && idMap.has(reference.substring(1))) {
                          element.setAttribute(attributeName, "#" + idMap.get(reference.substring(1)));
                        }
                      }
                      for (const attributeName of ["fill", "stroke", "filter", "clip-path", "mask", "marker-start", "marker-mid", "marker-end", "style"]) {
                        const value = element.getAttribute(attributeName);
                        if (value?.includes("url(#")) {
                          element.setAttribute(attributeName, value.replace(/url\\(#([^)]+)\\)/g,
                            (match, referencedId) => idMap.has(referencedId) ? "url(#" + idMap.get(referencedId) + ")" : match));
                        }
                      }
                      for (const attributeName of ["aria-labelledby", "aria-describedby"]) {
                        const value = element.getAttribute(attributeName);
                        if (value) {
                          element.setAttribute(attributeName, value.split(/\\s+/).map(referencedId => idMap.get(referencedId) ?? referencedId).join(" "));
                        }
                      }
                    }
                    const fitGraph = () => {
                      panZoom.resize();
                      panZoom.fit();
                      panZoom.center();
                    };
                    requestAnimationFrame(fitGraph);
                    if ("ResizeObserver" in window) {
                      new ResizeObserver(() => requestAnimationFrame(fitGraph)).observe(graphContainer);
                    }
                  } catch (error) {
                    container.textContent = "Reaction graph could not be rendered: " + error;
                    console.error(error);
                  }
                </script>
                """.formatted(id, escapedDot, vizJsUrl, svgPanZoomJsUrl);
        return new ConfluenceReactionGraph(graph.title(), wrapInCdata(html));
    }

    static String wrapInCdata(String value) {
        return "<![CDATA[" + value.replace("]]>", "]]]]><![CDATA[>") + "]]>";
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
}
