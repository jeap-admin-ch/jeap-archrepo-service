package ch.admin.bit.jeap.archrepo.docgen.graph.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphDto {
    private List<NodeDto> nodes;
    private List<EdgeDto> edges;

    public String toDot() {
        return toDot(node -> null);
    }

    public String toDot(Function<NodeDto, String> linkResolver) {
        StringBuilder dot = new StringBuilder("digraph G {\n");
        dot.append("  rankdir=LR;\n");
        dot.append("  graph [bgcolor=\"transparent\"];\n");

        // Add clusters for reactions grouped by trigger and component
        dot.append(buildReactionClusters(linkResolver));

        // Add message nodes
        appendMessageNodes(dot, linkResolver);

        // Add edges
        appendEdges(dot);

        dot.append("}\n");
        return dot.toString();
    }

    /**
     * Builds clusters for reactions grouped by trigger ID and component.
     * Reactions without triggers are added individually.
     */
    private String buildReactionClusters(Function<NodeDto, String> linkResolver) {
        StringBuilder dot = new StringBuilder();

        Map<Long, Map<String, List<ReactionNodeDto>>> groupedByTrigger = groupReactionsByTrigger();

        // Create clusters for grouped reactions
        for (Map.Entry<Long, Map<String, List<ReactionNodeDto>>> triggerEntry : groupedByTrigger.entrySet()) {
            long triggerId = triggerEntry.getKey();
            for (Map.Entry<String, List<ReactionNodeDto>> compEntry : triggerEntry.getValue().entrySet()) {
                String componentName = compEntry.getKey();
                List<ReactionNodeDto> reactions = compEntry.getValue();

                if (reactions.size() > 1) {
                    dot.append(buildCluster(triggerId, componentName, reactions, linkResolver));
                } else {
                    ReactionNodeDto node = reactions.getFirst();
                    node.setPartOfCluster(false);
                    dot.append(node.toDot(linkResolver.apply(node))).append("\n");
                }
            }
        }

        // Add reactions without any trigger
        appendUnlinkedReactions(dot, groupedByTrigger, linkResolver);

        return dot.toString();
    }

    private Map<Long, Map<String, List<ReactionNodeDto>>> groupReactionsByTrigger() {
        Map<Long, Map<String, List<ReactionNodeDto>>> groupedByTrigger = new HashMap<>();
        Map<Long, ReactionNodeDto> reactionsById = nodes.stream()
                .filter(ReactionNodeDto.class::isInstance)
                .map(ReactionNodeDto.class::cast)
                .collect(Collectors.toMap(ReactionNodeDto::getId, reaction -> reaction, (first, ignored) -> first));

        for (EdgeDto edge : edges) {
            if (edge instanceof TriggerEdgeDto triggerEdge) {
                long triggerId = triggerEdge.getSourceId();
                long reactionId = triggerEdge.getTargetReactionId();

                ReactionNodeDto reaction = reactionsById.get(reactionId);
                if (reaction != null) {
                    String componentName = normalizeComponentName(reaction.getComponent());
                    groupedByTrigger
                            .computeIfAbsent(triggerId, k -> new HashMap<>())
                            .computeIfAbsent(componentName, k -> new ArrayList<>())
                            .add(reaction);
                }
            }
        }
        return groupedByTrigger;
    }

    /**
     * Builds a DOT subgraph cluster for a set of reactions under the same trigger and component.
     */
    private String buildCluster(long triggerId, String componentName, List<ReactionNodeDto> reactions,
                                Function<NodeDto, String> linkResolver) {
        StringBuilder cluster = new StringBuilder();
        String escapedComponentName = NodeDto.escapeDotString(componentName);
        cluster.append(String.format("  subgraph \"cluster_trigger_%d_%s\" {\n", triggerId, escapedComponentName));
        cluster.append(String.format("    label=\"%s\";\n", escapedComponentName));
        cluster.append("    style=\"dashed,rounded\";\n");
        cluster.append("    color=\"#636363\";\n");
        cluster.append("    fontcolor=\"#636363\";\n");

        for (ReactionNodeDto node : reactions) {
            node.setPartOfCluster(true);
            cluster.append(node.toDot(linkResolver.apply(node))).append("\n");
        }
        cluster.append("  }\n");
        return cluster.toString();
    }

    private void appendUnlinkedReactions(StringBuilder dot, Map<Long, Map<String, List<ReactionNodeDto>>> groupedByTrigger,
                                         Function<NodeDto, String> linkResolver) {
        Set<Long> clusteredIds = groupedByTrigger.values().stream()
                .flatMap(map -> map.values().stream())
                .flatMap(List::stream)
                .map(ReactionNodeDto::getId)
                .collect(Collectors.toSet());

        nodes.stream()
                .filter(n -> n instanceof ReactionNodeDto)
                .map(n -> (ReactionNodeDto) n)
                .filter(r -> !clusteredIds.contains(r.getId()))
                .forEach(r -> {
                    r.setPartOfCluster(false);
                    dot.append(r.toDot(linkResolver.apply(r))).append("\n");
                });
    }

    private void appendMessageNodes(StringBuilder dot, Function<NodeDto, String> linkResolver) {
        nodes.stream()
                .filter(n -> n instanceof MessageNodeDto)
                .forEach(n -> dot.append(n.toDot(linkResolver.apply(n))).append("\n"));
    }

    private void appendEdges(StringBuilder dot) {
        edges.forEach(e -> dot.append(e.toDot()).append("\n"));
    }

    private String normalizeComponentName(String component) {
        return component.replaceAll("(-scs|-service)$", "");
    }
}
