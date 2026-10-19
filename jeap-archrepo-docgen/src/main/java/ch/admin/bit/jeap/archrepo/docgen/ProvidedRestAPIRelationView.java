package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import lombok.Builder;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@Builder(access = PRIVATE)
public class ProvidedRestAPIRelationView {

    String path;

    String method;

    String consumer;

    String pactUrl;

    static ProvidedRestAPIRelationView of(RestApiRelation relation) {
        return ProvidedRestAPIRelationView.builder()
                .consumer(relation.getConsumerName())
                .path(relation.getRestApi().getPath())
                .method(relation.getRestApi().getMethod())
                .pactUrl(relation.getPactUrl())
                .build();
    }
}
