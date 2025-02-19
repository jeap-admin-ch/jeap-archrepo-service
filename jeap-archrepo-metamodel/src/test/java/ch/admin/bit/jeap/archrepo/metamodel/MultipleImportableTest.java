package ch.admin.bit.jeap.archrepo.metamodel;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class MultipleImportableTest {

    @Test
    void filterByImportedOnlyBy_found() {
        //given
        RestApiRelation restApiRelationMultiple = RestApiRelation.builder().consumerName("multiple").importer(Importer.PACT_BROKER).lastSeen(ZonedDateTime.now()).build();
        restApiRelationMultiple.addImporter(Importer.GRAFANA);

        List<MultipleImportable> importables = List.of(
                restApiRelationMultiple,
                RestApiRelation.builder().consumerName("pactBroker").importer(Importer.PACT_BROKER).lastSeen(ZonedDateTime.now()).build(),
                RestApiRelation.builder().consumerName("grafana").importer(Importer.GRAFANA).lastSeen(ZonedDateTime.now()).build());

        //when
        List<MultipleImportable> filterByImportedOnlyBy = MultipleImportable.filterByImportedOnlyByImporter(importables, Importer.PACT_BROKER);

        //then
        assertThat(filterByImportedOnlyBy.size(), is(1));
        assertThat(((RestApiRelation) filterByImportedOnlyBy.getFirst()).getConsumerName(), is("pactBroker"));
        assertThat(filterByImportedOnlyBy.contains(restApiRelationMultiple), is (false));
    }
}
