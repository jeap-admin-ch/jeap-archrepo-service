package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.metamodel.Relation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.util.StringUtils.hasText;

/**
 * View of a relation as rendered into the documentation template
 */
@Value
@Builder(access = PRIVATE)
public class RelationView {
    String counterpart;

    String label;

    RelationType type;

    boolean labelLinkable;

    @Builder.Default
    Map<String, String> links = Map.of();

    static RelationView ofConsumedRelation(Relation relation) {
        return of(relation, relation.getProviderName());
    }

    static RelationView ofProvidedRelation(Relation relation) {
        return of(relation, relation.getConsumerName());
    }

    static RelationView of(Relation relation, String providerName) {
        return RelationView.builder()
                .counterpart(emptyStringAsNull(providerName))
                .label(relation.getLabel())
                .labelLinkable(relation.isLabelLinkable())
                .links(links(relation))
                .type(relation.getType())
                .build();
    }

    private static Map<String, String> links(Relation relation) {
        if (relation instanceof RestApiRelation restApiRelation && hasText(restApiRelation.getPactUrl())) {
            return Map.of("Pact", restApiRelation.getPactUrl());
        }
        return Map.of();
    }

    private static String emptyStringAsNull(String str) {
        return !hasText(str) ? null : str;
    }
}
