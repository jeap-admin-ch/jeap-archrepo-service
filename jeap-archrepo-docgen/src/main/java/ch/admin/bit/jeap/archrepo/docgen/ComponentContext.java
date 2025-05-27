package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Relation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import com.google.common.collect.Streams;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;

/**
 * The context of a system component, contains the component itself and relations to its direct neighbors
 */
@Value
@Builder(access = PRIVATE)
public class ComponentContext {

    private static final Comparator<Relation> RELATION_COMPARATOR =
            comparing(Relation::getConsumerName, nullsLast(naturalOrder()))
                    .thenComparing(Relation::getProviderName, nullsLast(naturalOrder()))
                    .thenComparing(Relation::getLabel);

    SystemComponent systemComponent;
    String openApiSpecUrl;

    List<RelationView> consumedRestApiRelations;
    List<RelationView> producedEventRelations;
    List<RelationView> sentCommandRelations;

    List<RelationView> providedRestApiRelations;
    List<ProvidedRestAPIRelationView> providedRestApiRelationViews;
    List<RelationView> consumedEventRelations;
    List<RelationView> receivedCommandRelations;
    List<ReactionStatisticsView> reactionStatisticsViews;

    public static ComponentContext of(ArchitectureModel model, SystemComponent systemComponent) {
        List<RelationView> consumedRestApiRelations = getConsumedRelationsByType(model, systemComponent, RelationType.REST_API_RELATION);
        List<RelationView> producedEventRelations = getProvidedRelationsByType(model, systemComponent, RelationType.EVENT_RELATION);
        List<RelationView> sentCommandRelations = getProvidedRelationsByType(model, systemComponent, RelationType.COMMAND_RELATION);

        List<RelationView> providedRestApiRelations = getProvidedRelationsByType(model, systemComponent, RelationType.REST_API_RELATION);
        List<ProvidedRestAPIRelationView> providedRestApiRelationViews = getProvidedRestApiRelations(model, systemComponent);
        List<RelationView> consumedEventRelations = getConsumedRelationsByType(model, systemComponent, RelationType.EVENT_RELATION);
        List<RelationView> receivedCommandRelations = getConsumedRelationsByType(model, systemComponent, RelationType.COMMAND_RELATION);

        String openApiSpecUrl = getOpenApiUrl(model, systemComponent);
        List<ReactionStatisticsView> reactionStatisticsViews = getReactionStatisticsViews(systemComponent);

        return ComponentContext.builder()
                .systemComponent(systemComponent)
                .consumedRestApiRelations(consumedRestApiRelations)
                .producedEventRelations(producedEventRelations)
                .sentCommandRelations(sentCommandRelations)
                .providedRestApiRelations(providedRestApiRelations)
                .providedRestApiRelationViews(providedRestApiRelationViews)
                .consumedEventRelations(consumedEventRelations)
                .receivedCommandRelations(receivedCommandRelations)
                .openApiSpecUrl(openApiSpecUrl)
                .reactionStatisticsViews(reactionStatisticsViews)
                .build();
    }

    private static List<ReactionStatisticsView> getReactionStatisticsViews(SystemComponent systemComponent) {
        return systemComponent.getReactionStatistics().isEmpty() ?
                Collections.emptyList() :
                systemComponent.getReactionStatistics().stream()
                        .map(ReactionStatisticsView::of)
                        .collect(Collectors.toList());
    }

    public Set<String> getComponentsInContext() {
        return Streams.concat(
                        consumedEventRelations.stream(),
                        producedEventRelations.stream(),
                        sentCommandRelations.stream(),
                        providedRestApiRelations.stream(),
                        consumedRestApiRelations.stream(),
                        receivedCommandRelations.stream())
                .map(RelationView::getCounterpart)
                .filter(StringUtils::isNotEmpty)
                .filter(name -> !systemComponent.getName().equals(name))
                .collect(toSet());
    }

    public Map<RelationView, List<RelationView>> getProducedEventsGroupedByEvent() {
        return getRelationViewGroupedRecord(producedEventRelations);
    }

    public Map<RelationView, List<RelationView>> getSentCommandsGroupedByCommand() {
        return getRelationViewGroupedRecord(sentCommandRelations);
    }

    public Map<RelationView, List<RelationView>> getConsumedEventsGroupedByEvent() {
        return getRelationViewGroupedRecord(consumedEventRelations);
    }

    public Map<RelationView, List<RelationView>> getReceivedCommandsGroupedByCommand() {
        return getRelationViewGroupedRecord(receivedCommandRelations);
    }

    public List<RestApiGroupedResult> getProvidedRestApiRelationsGroupedByPath() {
        //List of all rest apis formatted for rendering in html table
        List<RestApiGroupedResult> result = new ArrayList<>();

        for (Map.Entry<String, List<ProvidedRestAPIRelationView>> entry : getProvidedRestApiRelationViewsGroupedByPath().entrySet()) {
            Map<String, List<ProvidedRestAPIRelationView>> groupedByMethod = getProvidedRestApiRelationViewsGroupedByMethod(entry.getValue());
            boolean firstRow = true;
            for (Map.Entry<String, List<ProvidedRestAPIRelationView>> relation : groupedByMethod.entrySet()) {
                if (firstRow) {
                    //first row contains the rowspan value
                    result.add(new RestApiGroupedResult(new RestApiKey(entry.getKey(), groupedByMethod.keySet().size()), relation.getKey(), relation.getValue()));
                    firstRow = false;
                } else {
                    //other rows have a null value for the first column
                    result.add(new RestApiGroupedResult(null, relation.getKey(), relation.getValue()));
                }
            }
        }
        return result;
    }

    private Map<String, List<ProvidedRestAPIRelationView>> getProvidedRestApiRelationViewsGroupedByPath() {
        return new TreeMap<>(providedRestApiRelationViews.stream().collect(groupingBy(ProvidedRestAPIRelationView::getPath)));
    }

    private Map<String, List<ProvidedRestAPIRelationView>> getProvidedRestApiRelationViewsGroupedByMethod(List<ProvidedRestAPIRelationView> relations) {
        return new TreeMap<>(relations.stream().collect(groupingBy(ProvidedRestAPIRelationView::getMethod)));
    }

    private static Map<RelationView, List<RelationView>> getRelationViewGroupedRecord(List<RelationView> relationViews) {
        Map<RelationView, List<RelationView>> result = new TreeMap<>(comparing(o -> o.getLabel().toLowerCase()));
        for (Map.Entry<String, List<RelationView>> entry : relationViews.stream().collect(groupingBy(RelationView::getLabel)).entrySet()) {
            RelationView relationView = entry.getValue().getFirst();
            result.put(relationView, entry.getValue().stream().distinct().collect(Collectors.toList()));
        }
        return result;
    }

    private static List<RelationView> getConsumedRelationsByType(ArchitectureModel model, SystemComponent systemComponent, RelationType relationType) {
        return model.getAllActiveRelationsByType(relationType).stream()
                .filter(relation -> systemComponent.getName().equals(relation.getConsumerName()))
                .sorted(RELATION_COMPARATOR)
                .map(RelationView::ofConsumedRelation)
                .toList();
    }

    private static List<RelationView> getProvidedRelationsByType(ArchitectureModel model, SystemComponent systemComponent, RelationType relationType) {
        return model.getAllActiveRelationsByType(relationType).stream()
                .filter(relation -> systemComponent.getName().equals(relation.getProviderName()))
                .sorted(RELATION_COMPARATOR)
                .map(RelationView::ofProvidedRelation)
                .toList();
    }

    private static List<ProvidedRestAPIRelationView> getProvidedRestApiRelations(ArchitectureModel model, SystemComponent systemComponent) {
        return model.getAllActiveRelationsByType(RelationType.REST_API_RELATION).stream()
                .filter(relation -> systemComponent.getName().equals(relation.getProviderName()))
                .map(RestApiRelation.class::cast)
                .map(ProvidedRestAPIRelationView::of)
                .toList();
    }

    private static String getOpenApiUrl(ArchitectureModel model, SystemComponent systemComponent) {
        Optional<String> restApi = model.getRestApiForSystemComponent(systemComponent);
        return restApi.orElse("");
    }


}
