package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Relation;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;
import com.google.common.collect.Streams;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static java.util.Comparator.*;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;

/**
 * The context of a system component, contains the component itself and relations to its direct neighbors
 */
@Value
@Builder(access = PRIVATE)
public class SystemContext {
    private static final Comparator<Relation> RELATION_COMPARATOR =
            comparing(Relation::getConsumerName, nullsLast(naturalOrder()))
                    .thenComparing(Relation::getProviderName, nullsLast(naturalOrder()))
                    .thenComparing(Relation::getLabel);

    System system;

    List<RelationView> consumedRestApiRelations;
    List<RelationView> producedEventRelations;
    List<RelationView> sentCommandRelations;

    List<RelationView> providedRestApiRelations;
    List<RelationView> consumedEventRelations;
    List<RelationView> receivedCommandRelations;

    public static SystemContext of(ArchitectureModel model, System system) {
        List<RelationView> consumedRestApiRelations = getConsumedRelationsByType(model, system, RelationType.REST_API_RELATION);
        List<RelationView> producedEventRelations = getProvidedRelationsByType(model, system, RelationType.EVENT_RELATION);
        List<RelationView> sentCommandRelations = getProvidedRelationsByType(model, system, RelationType.COMMAND_RELATION);

        List<RelationView> providedRestApiRelations = getProvidedRelationsByType(model, system, RelationType.REST_API_RELATION);
        List<RelationView> consumedEventRelations = getConsumedRelationsByType(model, system, RelationType.EVENT_RELATION);
        List<RelationView> receivedCommandRelations = getConsumedRelationsByType(model, system, RelationType.COMMAND_RELATION);

        return SystemContext.builder()
                .system(system)
                .consumedRestApiRelations(consumedRestApiRelations)
                .producedEventRelations(producedEventRelations)
                .sentCommandRelations(sentCommandRelations)
                .providedRestApiRelations(providedRestApiRelations)
                .consumedEventRelations(consumedEventRelations)
                .receivedCommandRelations(receivedCommandRelations)
                .build();
    }

    public Set<String> getSystemsInContext() {
        return Streams.concat(
                consumedEventRelations.stream(),
                producedEventRelations.stream(),
                sentCommandRelations.stream(),
                providedRestApiRelations.stream(),
                consumedRestApiRelations.stream(),
                receivedCommandRelations.stream())
                .map(RelationView::getCounterpart)
                .filter(StringUtils::isNotEmpty)
                .filter(name -> !system.getName().equals(name))
                .collect(toSet());
    }

    private static List<RelationView> getConsumedRelationsByType(ArchitectureModel model, System system, RelationType relationType) {
        return model.getAllActiveRelationsByType(relationType).stream()
                .filter(relation -> relationToOrFromComponentInSystem(model, system, relation))
                .sorted(RELATION_COMPARATOR)
                .map(relation -> RelationView.of(relation, systemName(model, relation.getProviderName())))
                .filter(relation -> !system.getName().equals(relation.getCounterpart()))
                .toList();
    }

    private static List<RelationView> getProvidedRelationsByType(ArchitectureModel model, System system, RelationType relationType) {
        return model.getAllActiveRelationsByType(relationType).stream()
                .filter(relation -> relationToOrFromComponentInSystem(model, system, relation))
                .sorted(RELATION_COMPARATOR)
                .map(relation -> RelationView.of(relation, systemName(model, relation.getConsumerName())))
                .filter(relation -> !system.getName().equals(relation.getCounterpart()))
                .toList();
    }

    private static String systemName(ArchitectureModel model, String componentName) {
        if (StringUtils.isEmpty(componentName)) {
            return null;
        }
        return model.findSystemComponent(componentName)
                .map(systemComponent -> systemComponent.getParent().getName())
                .orElse(null);
    }

    private static boolean relationToOrFromComponentInSystem(ArchitectureModel model, System system, Relation relation) {
        String consumerName = relation.getConsumerName();
        String providerName = relation.getProviderName();
        return partOfSystem(model, system, consumerName) || partOfSystem(model, system, providerName);
    }

    private static boolean partOfSystem(ArchitectureModel model, System system, String componentName) {
        if (StringUtils.isEmpty(componentName)) {
            return false;
        }
        return model.findSystemComponent(componentName)
                .map(component -> system.equals(component.getParent()))
                .orElse(false);
    }
}
