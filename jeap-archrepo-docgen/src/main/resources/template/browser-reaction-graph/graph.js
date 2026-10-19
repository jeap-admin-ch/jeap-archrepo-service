  const graphContainer = document.getElementById("@@GRAPH_ID@@");
  const container = document.getElementById("@@GRAPH_ID@@-content");
  const dot = document.getElementById("@@GRAPH_ID@@-source").value;
  let nodeCount = Number.parseInt(graphContainer.dataset.nodeCount, 10);
  const minimapThreshold = 20;
  const mapToggle = graphContainer.querySelector(".reaction-graph-map-toggle");
  const minimap = graphContainer.querySelector(".reaction-graph-minimap");
  const minimapClose = graphContainer.querySelector(".reaction-graph-minimap-close");
  const search = graphContainer.querySelector(".reaction-graph-search");
  const searchInput = search.querySelector("input");
  const searchCount = search.querySelector(".reaction-graph-search-count");
  const navigationStatus = graphContainer.querySelector(".reaction-graph-navigation-status");
  const searchNavigationButtons = search.querySelectorAll('[data-search-action="previous"], [data-search-action="next"]');
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
  const themeObserver = new MutationObserver(applyTheme);
  themeObserver.observe(themeRoot, {
    attributes: true,
    attributeFilter: ["data-color-mode", "data-theme"]
  });
  colorScheme.addEventListener("change", applyTheme);
  try {
    const runtime = window.__jeapArchRepoReactionGraphRuntime ??= Promise.all([
      import(@@VIZ_JS_URL@@),
      import(@@SVG_PAN_ZOOM_JS_URL@@)
    ]).then(async ([vizModule, panZoomModule]) => ({
      viz: await vizModule.instance(),
      svgPanZoom: panZoomModule.default
    }));
    const { viz, svgPanZoom } = await runtime;
    reactionGraphInitialization: {
    if (!graphContainer.isConnected) {
      themeObserver.disconnect();
      colorScheme.removeEventListener("change", applyTheme);
      break reactionGraphInitialization;
    }
    const svg = viz.renderSVGElement(dot);
    nodeCount = Math.max(Number.isFinite(nodeCount) ? nodeCount : 0, svg.querySelectorAll("g.node").length);
    graphContainer.dataset.nodeCount = String(nodeCount);
    const originalViewBox = svg.viewBox.baseVal;
    const graphBounds = {
      x: originalViewBox.x,
      y: originalViewBox.y,
      width: originalViewBox.width,
      height: originalViewBox.height
    };
    const namespaceSvgIds = (targetSvg, prefix) => {
      const idMap = new Map();
      for (const element of [targetSvg, ...targetSvg.querySelectorAll("[id]")]) {
        if (element.id) {
          const namespacedId = prefix + element.id;
          idMap.set(element.id, namespacedId);
          element.id = namespacedId;
        }
      }
      for (const element of targetSvg.querySelectorAll("*")) {
        for (const attributeName of ["href", "xlink:href"]) {
          const reference = element.getAttribute(attributeName);
          if (reference?.startsWith("#") && idMap.has(reference.substring(1))) {
            element.setAttribute(attributeName, "#" + idMap.get(reference.substring(1)));
          }
        }
        for (const attributeName of ["fill", "stroke", "filter", "clip-path", "mask", "marker-start", "marker-mid", "marker-end", "style"]) {
          const value = element.getAttribute(attributeName);
          if (value?.includes("url(#")) {
            element.setAttribute(attributeName, value.replace(/url\(#([^)]+)\)/g,
              (match, referencedId) => idMap.has(referencedId) ? "url(#" + idMap.get(referencedId) + ")" : match));
          }
        }
        for (const attributeName of ["aria-labelledby", "aria-describedby"]) {
          const value = element.getAttribute(attributeName);
          if (value) {
            element.setAttribute(attributeName, value.split(/\s+/).map(referencedId => idMap.get(referencedId) ?? referencedId).join(" "));
          }
        }
      }
    };
    namespaceSvgIds(svg, "@@GRAPH_ID@@-");
    if (graphBounds.width > 0 && graphBounds.height > 0) {
      if (nodeCount > minimapThreshold) {
        graphContainer.style.height = "clamp(480px, 70vh, 760px)";
      } else {
        const naturalScale = Math.min(1, graphContainer.clientWidth / graphBounds.width);
        const compactHeight = Math.ceil(graphBounds.height * naturalScale + 56);
        graphContainer.style.height = Math.min(480, Math.max(220, compactHeight)) + "px";
      }
    }
    svg.removeAttribute("width");
    svg.removeAttribute("height");
    svg.style.width = "100%";
    svg.style.height = "100%";
    container.replaceChildren(svg);
    const searchableNodes = [...svg.querySelectorAll("g.node")].map((element, index) => {
      element.dataset.searchIndex = String(index);
      const text = [...element.querySelectorAll("text")]
        .map(label => label.textContent ?? "")
        .join(" ")
        .toLowerCase();
      return { element, text };
    });
    let minimapSvg = null;
    let viewportRect = null;
    let lastViewportMatrix = null;
    const panZoom = svgPanZoom(svg, {
      panEnabled: true,
      zoomEnabled: true,
      mouseWheelZoomEnabled: true,
      controlIconsEnabled: false,
      fit: true,
      center: true,
      minZoom: 0.01,
      maxZoom: 10000
    });
    let tornDown = false;
    const animationFrames = new Set();
    const scheduleFrame = callback => {
      if (tornDown) return;
      let completedSynchronously = false;
      let frameId;
      frameId = window.requestAnimationFrame(timestamp => {
        completedSynchronously = true;
        animationFrames.delete(frameId);
        if (!tornDown && graphContainer.isConnected) callback(timestamp);
      });
      if (!completedSynchronously) animationFrames.add(frameId);
    };
    const fitScale = () => {
      const sizes = panZoom.getSizes();
      return Math.min(sizes.width / sizes.viewBox.width, sizes.height / sizes.viewBox.height);
    };
    const configureZoomLimits = () => {
      const fitRealZoom = fitScale();
      if (!(fitRealZoom > 0) || !Number.isFinite(fitRealZoom)) {
        return null;
      }
      const readableRelativeZoom = Math.max(1, 0.8 / fitRealZoom);
      const minimumRelativeZoom = Math.min(1, 1 / fitRealZoom);
      panZoom.setMinZoom(minimumRelativeZoom);
      panZoom.setMaxZoom(Math.max(4, readableRelativeZoom * 4, 2 / fitRealZoom));
      return { fitRealZoom, readableRelativeZoom };
    };
    const updateMinimap = matrix => {
      lastViewportMatrix = matrix;
      if (!viewportRect || !(matrix.a > 0) || !(matrix.d > 0)) {
        return;
      }
      const sizes = panZoom.getSizes();
      viewportRect.setAttribute("x", String(-matrix.e / matrix.a));
      viewportRect.setAttribute("y", String(-matrix.f / matrix.d));
      viewportRect.setAttribute("width", String(sizes.width / matrix.a));
      viewportRect.setAttribute("height", String(sizes.height / matrix.d));
    };
    panZoom.setOnUpdatedCTM(updateMinimap);
    const ensureMinimap = () => {
      if (minimapSvg) return;
      minimapSvg = svg.cloneNode(true);
      minimapSvg.setAttribute("viewBox", [graphBounds.x, graphBounds.y, graphBounds.width, graphBounds.height].join(" "));
      minimapSvg.removeAttribute("transform");
      minimapSvg.style.removeProperty("transform");
      const minimapViewport = minimapSvg.querySelector(".svg-pan-zoom_viewport");
      if (minimapViewport) {
        minimapViewport.removeAttribute("transform");
        minimapViewport.style.removeProperty("transform");
        minimapViewport.style.removeProperty("-ms-transform");
        minimapViewport.style.removeProperty("-webkit-transform");
      }
      minimapSvg.querySelectorAll("text, title").forEach(element => element.remove());
      minimapSvg.querySelectorAll("a").forEach(link => link.replaceWith(...link.childNodes));
      namespaceSvgIds(minimapSvg, "@@GRAPH_ID@@-minimap-");
      minimapSvg.setAttribute("aria-hidden", "true");
      minimapSvg.setAttribute("focusable", "false");
      viewportRect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
      viewportRect.setAttribute("class", "reaction-graph-minimap-viewport");
      minimapSvg.appendChild(viewportRect);
      minimap.prepend(minimapSvg);
      const graphPoint = event => {
        const point = minimapSvg.createSVGPoint();
        point.x = event.clientX;
        point.y = event.clientY;
        return point.matrixTransform(minimapSvg.getScreenCTM().inverse());
      };
      const centerAt = point => {
        const sizes = panZoom.getSizes();
        panZoom.pan({
          x: sizes.width / 2 - point.x * sizes.realZoom,
          y: sizes.height / 2 - point.y * sizes.realZoom
        });
      };
      minimapSvg.addEventListener("click", event => {
        if (event.target !== viewportRect) centerAt(graphPoint(event));
      });
      let previousDragPoint = null;
      viewportRect.addEventListener("pointerdown", event => {
        previousDragPoint = graphPoint(event);
        viewportRect.setPointerCapture(event.pointerId);
        event.preventDefault();
      });
      viewportRect.addEventListener("pointermove", event => {
        if (!previousDragPoint) return;
        const nextPoint = graphPoint(event);
        const realZoom = panZoom.getSizes().realZoom;
        panZoom.panBy({
          x: -(nextPoint.x - previousDragPoint.x) * realZoom,
          y: -(nextPoint.y - previousDragPoint.y) * realZoom
        });
        previousDragPoint = nextPoint;
      });
      const endDrag = () => previousDragPoint = null;
      viewportRect.addEventListener("pointerup", endDrag);
      viewportRect.addEventListener("pointercancel", endDrag);
      minimap.addEventListener("keydown", event => {
        const distance = 60;
        if (event.key === "ArrowLeft") panZoom.panBy({ x: distance, y: 0 });
        else if (event.key === "ArrowRight") panZoom.panBy({ x: -distance, y: 0 });
        else if (event.key === "ArrowUp") panZoom.panBy({ x: 0, y: distance });
        else if (event.key === "ArrowDown") panZoom.panBy({ x: 0, y: -distance });
        else if (event.key === "Home") showOverview();
        else return;
        event.preventDefault();
      });
      if (lastViewportMatrix) updateMinimap(lastViewportMatrix);
    };
    let mapController = null;
    const setMinimapExpanded = (expanded, restoreFocus = false) => {
      if (expanded) {
        const activeMap = window.__jeapArchRepoActiveReactionGraphMap;
        if (activeMap && activeMap !== mapController) activeMap.close(false);
        window.__jeapArchRepoActiveReactionGraphMap = mapController;
      } else if (window.__jeapArchRepoActiveReactionGraphMap === mapController) {
        delete window.__jeapArchRepoActiveReactionGraphMap;
      }
      if (expanded) ensureMinimap();
      minimap.hidden = !expanded;
      mapToggle.hidden = expanded;
      mapToggle.setAttribute("aria-expanded", String(expanded));
      if (expanded) minimapClose.focus();
      if (!expanded && restoreFocus) mapToggle.focus();
      if (expanded && lastViewportMatrix) updateMinimap(lastViewportMatrix);
    };
    mapController = { close: restoreFocus => setMinimapExpanded(false, restoreFocus) };
    if (nodeCount > minimapThreshold && graphBounds.width > 0 && graphBounds.height > 0) {
      mapToggle.hidden = false;
      mapToggle.addEventListener("click", () => {
        setMinimapExpanded(mapToggle.getAttribute("aria-expanded") !== "true");
      });
      minimapClose.addEventListener("click", () => mapController.close(true));
    }
    const showOverview = () => {
      panZoom.resize();
      configureZoomLimits();
      panZoom.fit();
      panZoom.center();
    };
    const showInitialView = () => {
      panZoom.resize();
      panZoom.fit();
      const zoom = configureZoomLimits();
      if (zoom) {
        panZoom.zoom(nodeCount > minimapThreshold ? zoom.readableRelativeZoom : Math.min(1, 1 / zoom.fitRealZoom));
      }
      panZoom.center();
    };
    const centerNode = targetNode => {
      const currentFitScale = fitScale();
      configureZoomLimits();
      if (currentFitScale > 0) panZoom.zoom(1 / currentFitScale);
      scheduleFrame(() => scheduleFrame(() => {
        const graphRect = container.getBoundingClientRect();
        const nodeRect = targetNode.getBoundingClientRect();
        panZoom.panBy({
          x: graphRect.left + graphRect.width / 2 - nodeRect.left - nodeRect.width / 2,
          y: graphRect.top + graphRect.height / 2 - nodeRect.top - nodeRect.height / 2
        });
      }));
    };
    let searchMatches = [];
    let currentSearchMatch = -1;
    const minimapSearchNode = index =>
      minimapSvg?.querySelector('[data-search-index="' + index + '"]');
    const updateSearchHighlights = () => {
      for (const { element } of searchableNodes) {
        element.classList.remove("reaction-graph-search-match", "reaction-graph-search-current");
      }
      minimapSvg?.querySelectorAll(".reaction-graph-search-match, .reaction-graph-search-current")
        .forEach(element => element.classList.remove("reaction-graph-search-match", "reaction-graph-search-current"));
      for (const index of searchMatches) {
        searchableNodes[index].element.classList.add("reaction-graph-search-match");
        minimapSearchNode(index)?.classList.add("reaction-graph-search-match");
      }
      if (currentSearchMatch >= 0) {
        const currentIndex = searchMatches[currentSearchMatch];
        searchableNodes[currentIndex].element.classList.add("reaction-graph-search-current");
        minimapSearchNode(currentIndex)?.classList.add("reaction-graph-search-current");
      }
      searchCount.textContent = (currentSearchMatch >= 0 ? currentSearchMatch + 1 : 0) + "/" + searchMatches.length;
      searchNavigationButtons.forEach(button => button.disabled = searchMatches.length === 0);
    };
    const resetSearch = (restoreFocus = false) => {
      const wasVisible = !search.hidden;
      searchInput.value = "";
      searchMatches = [];
      currentSearchMatch = -1;
      search.hidden = true;
      graphContainer.classList.remove("reaction-graph-search-open");
      updateSearchHighlights();
      if (restoreFocus && wasVisible) graphContainer.focus({ preventScroll: true });
    };
    const updateSearch = () => {
      const query = searchInput.value.trim().toLowerCase();
      searchMatches = query ? searchableNodes
        .map(({ text }, index) => text.includes(query) ? index : -1)
        .filter(index => index >= 0) : [];
      currentSearchMatch = searchMatches.length > 0 ? 0 : -1;
      updateSearchHighlights();
      if (currentSearchMatch >= 0) centerNode(searchableNodes[searchMatches[currentSearchMatch]].element);
    };
    const navigateSearch = direction => {
      if (searchMatches.length === 0) return;
      currentSearchMatch = (currentSearchMatch + direction + searchMatches.length) % searchMatches.length;
      updateSearchHighlights();
      centerNode(searchableNodes[searchMatches[currentSearchMatch]].element);
    };
    const openSearch = () => {
      search.hidden = false;
      graphContainer.classList.add("reaction-graph-search-open");
      scheduleFrame(() => {
        searchInput.focus();
        searchInput.select();
      });
    };
    const searchController = { open: openSearch, reset: resetSearch };
    const activateSearchController = () => {
      const activeSearch = window.__jeapArchRepoActiveReactionGraphSearch;
      if (activeSearch && activeSearch !== searchController) activeSearch.reset();
      window.__jeapArchRepoActiveReactionGraphSearch = searchController;
    };
    const handleDocumentPointerDown = event => {
      if (graphContainer.contains(event.target)) {
        activateSearchController();
        if (!search.hidden && !search.contains(event.target)) resetSearch();
      } else {
        if (window.__jeapArchRepoActiveReactionGraphSearch === searchController) {
          delete window.__jeapArchRepoActiveReactionGraphSearch;
        }
        if (!search.hidden) resetSearch();
      }
    };
    const handleGraphFocusIn = activateSearchController;
    const handleGraphFocusOut = event => {
      if (event.relatedTarget && !graphContainer.contains(event.relatedTarget) &&
          window.__jeapArchRepoActiveReactionGraphSearch === searchController) {
        delete window.__jeapArchRepoActiveReactionGraphSearch;
        resetSearch();
      }
    };
    const handleWindowBlur = () => {
      resetSearch();
      if (window.__jeapArchRepoActiveReactionGraphSearch === searchController) {
        delete window.__jeapArchRepoActiveReactionGraphSearch;
      }
    };
    const handleDocumentKeyDown = event => {
      const searchIsActive = window.__jeapArchRepoActiveReactionGraphSearch === searchController;
      if (searchIsActive && (event.ctrlKey || event.metaKey) && !event.altKey && event.key.toLowerCase() === "f") {
        event.preventDefault();
        openSearch();
        return;
      }
      if (searchIsActive && !search.hidden && event.target === searchInput && event.key === "Enter") {
        event.preventDefault();
        navigateSearch(event.shiftKey ? -1 : 1);
        return;
      }
      if (searchIsActive && !search.hidden && event.key === "Escape") {
        event.preventDefault();
        resetSearch(true);
        return;
      }
      if (event.key === "Escape" && window.__jeapArchRepoActiveReactionGraphMap === mapController) {
        mapController.close(true);
        return;
      }
      if (searchIsActive && event.target === graphContainer) {
        const distance = 60;
        if (event.key === "ArrowLeft") panZoom.panBy({ x: distance, y: 0 });
        else if (event.key === "ArrowRight") panZoom.panBy({ x: -distance, y: 0 });
        else if (event.key === "ArrowUp") panZoom.panBy({ x: 0, y: distance });
        else if (event.key === "ArrowDown") panZoom.panBy({ x: 0, y: -distance });
        else if (event.key === "Home") showOverview();
        else return;
        event.preventDefault();
      }
    };
    searchInput.addEventListener("input", updateSearch);
    search.addEventListener("click", event => {
      const action = event.target.closest("button")?.dataset.searchAction;
      if (action === "previous") navigateSearch(-1);
      if (action === "next") navigateSearch(1);
      if (action === "close") resetSearch(true);
    });
    graphContainer.addEventListener("focusin", handleGraphFocusIn);
    graphContainer.addEventListener("focusout", handleGraphFocusOut);
    document.addEventListener("pointerdown", handleDocumentPointerDown);
    document.addEventListener("keydown", handleDocumentKeyDown);
    window.addEventListener("blur", handleWindowBlur);
    graphContainer.querySelector(".reaction-graph-controls").addEventListener("click", event => {
      const action = event.target.closest("button")?.dataset.action;
      if (action === "zoom-in") panZoom.zoomIn();
      if (action === "zoom-out") panZoom.zoomOut();
      if (action === "fit") showOverview();
      if (action === "actual-size") {
        const currentFitScale = fitScale();
        if (currentFitScale > 0) {
          panZoom.zoom(1 / currentFitScale);
          panZoom.center();
        }
      }
    });
    let navigationWindow = window;
    try {
      if (window.top.location.href) navigationWindow = window.top;
    } catch (ignored) {
      // Layered URL, referrer, and session transport below handles isolated macro frames.
    }
    const navigationPrefix = "#archrepo-graph?";
    const pendingNavigationKey = "jeapArchRepo.pendingGraphNavigation";
    const navigationPageId = value => {
      try {
        return new URL(value, window.location.href).searchParams.get("pageId");
      } catch (ignored) {
        return null;
      }
    };
    const normalizedPageUrl = value => {
      const url = new URL(value, window.location.href);
      url.hash = "";
      url.searchParams.delete("archrepoGraphNode");
      url.searchParams.delete("archrepoGraphVariant");
      return url.href;
    };
    const isSamePage = (targetUrl, currentUrl) => {
      const targetPageId = navigationPageId(targetUrl);
      const currentPageId = navigationPageId(currentUrl);
      if (targetPageId && currentPageId) {
        return targetUrl.origin === currentUrl.origin && targetPageId === currentPageId;
      }
      return normalizedPageUrl(targetUrl) === normalizedPageUrl(currentUrl);
    };
    const navigationHashFromUrl = value => {
      try {
        const url = new URL(value, window.location.href);
        const node = url.searchParams.get("archrepoGraphNode");
        if (!node) return "";
        const parameters = new URLSearchParams({ node });
        if (url.searchParams.has("archrepoGraphVariant")) {
          parameters.set("variant", url.searchParams.get("archrepoGraphVariant") ?? "");
        }
        return navigationPrefix + parameters;
      } catch (ignored) {
        return "";
      }
    };
    const pendingNavigationHash = () => {
      try {
        const pending = JSON.parse(sessionStorage.getItem(pendingNavigationKey));
        const age = Date.now() - Number(pending?.createdAt);
        if (!pending || !Number.isFinite(age) || age < 0 || age > 60000 ||
            typeof pending.hash !== "string" || !pending.hash.startsWith(navigationPrefix)) {
          sessionStorage.removeItem(pendingNavigationKey);
          return "";
        }
        const currentPageIds = [navigationWindow.location.href, window.location.href, document.referrer]
          .map(navigationPageId)
          .filter(pageId => pageId !== null);
        if (pending.pageId && currentPageIds.length > 0 && !currentPageIds.includes(pending.pageId)) {
          return "";
        }
        return pending.hash;
      } catch (ignored) {
        return "";
      }
    };
    const rememberNavigation = targetUrl => {
      try {
        sessionStorage.setItem(pendingNavigationKey, JSON.stringify({
          hash: targetUrl.hash,
          pageId: targetUrl.searchParams.get("pageId"),
          createdAt: Date.now()
        }));
      } catch (ignored) {
        // URL and referrer transport remain available when storage is blocked.
      }
    };
    const clearPendingNavigation = hash => {
      try {
        const pending = JSON.parse(sessionStorage.getItem(pendingNavigationKey));
        if (pending?.hash === hash) sessionStorage.removeItem(pendingNavigationKey);
      } catch (ignored) {
        // Storage can be unavailable in opaque sandbox origins.
      }
    };
    const currentNavigationHash = () => {
      for (const hash of [navigationWindow.location.hash, window.location.hash]) {
        if (hash.startsWith(navigationPrefix)) return hash;
      }
      for (const url of [navigationWindow.location.href, window.location.href, document.referrer]) {
        const hash = navigationHashFromUrl(url);
        if (hash) return hash;
      }
      return pendingNavigationHash();
    };
    const updateNavigationUrl = targetUrl => {
      try {
        if (navigationWindow === window && window !== window.top) return;
        const currentUrl = new URL(navigationWindow.location.href);
        for (const parameter of ["archrepoGraphNode", "archrepoGraphVariant"]) {
          if (targetUrl.searchParams.has(parameter)) {
            currentUrl.searchParams.set(parameter, targetUrl.searchParams.get(parameter) ?? "");
          } else {
            currentUrl.searchParams.delete(parameter);
          }
        }
        currentUrl.hash = targetUrl.hash;
        navigationWindow.history.replaceState(navigationWindow.history.state, "", currentUrl);
      } catch (ignored) {
        // Sandboxed frames cannot update their parent page URL.
      }
    };
    let focusedNode = null;
    let focusTimeout = null;
    const clearFocusedNode = () => {
      if (focusTimeout) window.clearTimeout(focusTimeout);
      focusedNode?.classList.remove("reaction-graph-target");
      focusedNode = null;
    };
    const readNavigationRequest = hash => {
      if (typeof hash !== "string" || !hash.startsWith(navigationPrefix)) return null;
      const parameters = new URLSearchParams(hash.substring(navigationPrefix.length));
      const node = parameters.get("node");
      return node ? {
        node,
        hasVariant: parameters.has("variant"),
        variant: parameters.get("variant") ?? ""
      } : null;
    };
    const resolveDeepLinkTarget = hash => {
      const request = readNavigationRequest(hash);
      if (!request || (request.hasVariant &&
          (!graphContainer.hasAttribute("data-navigation-key") ||
           graphContainer.dataset.navigationKey !== request.variant))) {
        return null;
      }
      const targetNode = [...svg.querySelectorAll("g.node")]
        .find(node => node.querySelector("title")?.textContent === request.node);
      return targetNode ? { request, targetNode } : null;
    };
    const applyDeepLink = (hash = currentNavigationHash()) => {
      clearFocusedNode();
      const target = resolveDeepLinkTarget(hash);
      if (!target) return;
      const { request, targetNode } = target;
      clearPendingNavigation(hash);
      graphContainer.scrollIntoView({ behavior: "instant", block: "center" });
      try {
        if (window.frameElement) window.frameElement.scrollIntoView({ behavior: "instant", block: "center" });
      } catch (ignored) {
        // Cross-origin parents cannot expose their frame element.
      }
      focusedNode = targetNode;
      focusedNode.classList.add("reaction-graph-target");
      centerNode(targetNode);
      navigationStatus.textContent = "Focused graph node " + request.node;
      const targetLink = targetNode.querySelector("a");
      if (targetLink) targetLink.focus({ preventScroll: true });
      else graphContainer.focus({ preventScroll: true });
      focusTimeout = window.setTimeout(clearFocusedNode, 5000);
    };
    scheduleFrame(() => {
      showInitialView();
      scheduleFrame(() => applyDeepLink());
    });
    const handleHashChange = () => scheduleFrame(() => applyDeepLink());
    navigationWindow?.addEventListener("hashchange", handleHashChange);
    const handleGraphLinkClick = event => {
      const link = event.target.closest("a");
      const href = link?.getAttribute("href") ?? link?.getAttribute("xlink:href");
      if (!href || !navigationWindow) return;
      const targetUrl = new URL(href, navigationWindow.location.href);
      const currentUrl = new URL(navigationWindow.location.href);
      const unmodifiedPrimaryClick = event.button === 0 &&
        !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey;
      if (unmodifiedPrimaryClick && targetUrl.hash.startsWith(navigationPrefix)) {
        rememberNavigation(targetUrl);
      }
      if (unmodifiedPrimaryClick && isSamePage(targetUrl, currentUrl) &&
          resolveDeepLinkTarget(targetUrl.hash)) {
        event.preventDefault();
        updateNavigationUrl(targetUrl);
        scheduleFrame(() => applyDeepLink(targetUrl.hash));
      }
    };
    svg.addEventListener("click", handleGraphLinkClick);
    let resizeObserver = null;
    if ("ResizeObserver" in window) {
      let previousWidth = graphContainer.clientWidth;
      let previousHeight = graphContainer.clientHeight;
      resizeObserver = new ResizeObserver(() => {
        const width = graphContainer.clientWidth;
        const height = graphContainer.clientHeight;
        if (width === previousWidth && height === previousHeight) return;
        previousWidth = width;
        previousHeight = height;
        if (!(width > 0) || !(height > 0)) return;
        const oldSizes = panZoom.getSizes();
        const oldPan = panZoom.getPan();
        const canPreserveView = oldSizes.width > 0 && oldSizes.height > 0 &&
          oldSizes.realZoom > 0 && Number.isFinite(oldSizes.realZoom) &&
          Number.isFinite(oldPan.x) && Number.isFinite(oldPan.y);
        if (!canPreserveView) {
          scheduleFrame(() => {
            showInitialView();
            scheduleFrame(() => applyDeepLink());
          });
          return;
        }
        const graphCenter = {
          x: (oldSizes.width / 2 - oldPan.x) / oldSizes.realZoom,
          y: (oldSizes.height / 2 - oldPan.y) / oldSizes.realZoom
        };
        const realZoom = oldSizes.realZoom;
        scheduleFrame(() => {
          panZoom.resize();
          const newFitScale = fitScale();
          configureZoomLimits();
          if (newFitScale > 0) panZoom.zoom(realZoom / newFitScale);
          const sizes = panZoom.getSizes();
          panZoom.pan({
            x: sizes.width / 2 - graphCenter.x * sizes.realZoom,
            y: sizes.height / 2 - graphCenter.y * sizes.realZoom
          });
        });
      });
      resizeObserver.observe(graphContainer);
    }
    let lifecycleObserver = null;
    const teardown = () => {
      if (tornDown) return;
      tornDown = true;
      for (const frameId of animationFrames) window.cancelAnimationFrame(frameId);
      animationFrames.clear();
      lifecycleObserver?.disconnect();
      resizeObserver?.disconnect();
      themeObserver.disconnect();
      colorScheme.removeEventListener("change", applyTheme);
      navigationWindow?.removeEventListener("hashchange", handleHashChange);
      graphContainer.removeEventListener("focusin", handleGraphFocusIn);
      graphContainer.removeEventListener("focusout", handleGraphFocusOut);
      document.removeEventListener("pointerdown", handleDocumentPointerDown);
      document.removeEventListener("keydown", handleDocumentKeyDown);
      window.removeEventListener("blur", handleWindowBlur);
      window.removeEventListener("pagehide", handlePageHide);
      if (window.__jeapArchRepoActiveReactionGraphMap === mapController) {
        delete window.__jeapArchRepoActiveReactionGraphMap;
      }
      if (window.__jeapArchRepoActiveReactionGraphSearch === searchController) {
        delete window.__jeapArchRepoActiveReactionGraphSearch;
      }
      resetSearch();
      clearFocusedNode();
      panZoom.destroy();
    };
    const handlePageHide = event => {
      if (!event.persisted) teardown();
    };
    window.addEventListener("pagehide", handlePageHide);
    lifecycleObserver = new MutationObserver(() => {
      if (!graphContainer.isConnected) teardown();
    });
    lifecycleObserver.observe(document.documentElement, { childList: true, subtree: true });
    }
  } catch (error) {
    themeObserver.disconnect();
    colorScheme.removeEventListener("change", applyTheme);
    container.textContent = "Reaction graph could not be rendered: " + error;
    console.error(error);
  }
