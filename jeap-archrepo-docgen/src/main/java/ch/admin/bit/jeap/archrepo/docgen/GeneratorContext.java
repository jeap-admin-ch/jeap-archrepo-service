package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.NodeDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.ReactionNodeDto;
import lombok.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;

@Value
class GeneratorContext {
    ArchitectureModel model;
    String rootPageId;
    String confluenceUrl;
    Set<String> generatedPageIds = new HashSet<>();
    Map<String, PageRef> systemPages = new HashMap<>();
    Map<String, PageRef> componentIndexPages = new HashMap<>();
    Map<String, PageRef> eventIndexPages = new HashMap<>();
    Map<String, PageRef> commandIndexPages = new HashMap<>();
    Map<String, PageRef> componentPages = new HashMap<>();
    Map<String, PageRef> messagePages = new HashMap<>();
    Set<String> ambiguousMessagePageKeys = new HashSet<>();
    Map<SystemComponent, PageRef> componentEntityPages = new IdentityHashMap<>();
    Map<MessageType, PageRef> messageEntityPages = new IdentityHashMap<>();

    void addGeneratedPageIds(String... pageIds) {
        generatedPageIds.addAll(Set.of(pageIds));
    }

    void addMessagePage(String messageTypeName, PageRef page) {
        String messageKey = key(messageTypeName);
        PageRef existingPage = messagePages.putIfAbsent(messageKey, page);
        if (existingPage != null && !existingPage.id().equals(page.id())) {
            ambiguousMessagePageKeys.add(messageKey);
        }
    }

    String resolveNodeUrl(NodeDto node) {
        String name = switch (node) {
            case MessageNodeDto message -> message.getMessageType();
            case ReactionNodeDto reaction -> reaction.getComponent();
            default -> null;
        };
        Map<String, PageRef> pages = node instanceof MessageNodeDto ? messagePages : componentPages;
        return Optional.ofNullable(name)
                .map(GeneratorContext::key)
                .filter(nodeKey -> !(node instanceof MessageNodeDto) || !ambiguousMessagePageKeys.contains(nodeKey))
                .map(pages::get)
                .map(page -> pageUrl(page.id(), node))
                .orElse(null);
    }

    private String pageUrl(String pageId, NodeDto node) {
        String baseUrl = confluenceUrl.endsWith("/") ? confluenceUrl.substring(0, confluenceUrl.length() - 1) : confluenceUrl;
        String encodedNodeId = encode(node.getDotId(), UTF_8);
        String navigationQuery = "&archrepoGraphNode=" + encodedNodeId;
        String fragment = "#archrepo-graph?node=" + encodedNodeId;
        if (node instanceof MessageNodeDto message) {
            String variant = MessageGraph.normalizeVariant(message.getMessageType(), message.getVariant());
            String encodedVariant = encode(variant, UTF_8);
            navigationQuery += "&archrepoGraphVariant=" + encodedVariant;
            fragment += "&variant=" + encodedVariant;
        }
        return baseUrl + "/pages/viewpage.action?pageId=" + pageId + navigationQuery + fragment;
    }

    static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    record PageRef(String id, String ancestorId, String title) {
    }
}
