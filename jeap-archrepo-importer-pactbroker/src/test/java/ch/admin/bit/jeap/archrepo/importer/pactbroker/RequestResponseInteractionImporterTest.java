package ch.admin.bit.jeap.archrepo.importer.pactbroker;

import au.com.dius.pact.core.model.Request;
import au.com.dius.pact.core.model.RequestResponseInteraction;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZonedDateTime;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

class RequestResponseInteractionImporterTest {

    private System system;

    private BackendService consumer;

    private BackendService provider;

    @Test
    void importInteraction() {

        RequestResponseInteractionImporter importer = new RequestResponseInteractionImporter(consumer, provider, "pactUrl");

        Request request = new Request("GET", "/foo/123-456-789/bar");
        RequestResponseInteraction interaction = new RequestResponseInteraction("desc", emptyList(), request);

        importer.importInteraction(interaction);
        importer.importInteraction(interaction);

        assertEquals(1, system.getRelations().size());
        RestApiRelation relation = (RestApiRelation) system.getRelations().getFirst();
        assertEquals(provider.getName(), relation.getProviderName());
        assertEquals(consumer.getName(), relation.getConsumerName());
        assertEquals("pactUrl", relation.getPactUrl());
        assertTrue(relation.getImporters().contains(Importer.PACT_BROKER));
        assertEquals("GET", relation.getRestApi().getMethod());
        assertEquals("/foo/123-456-789/bar", relation.getRestApi().getPath());

        assertEquals(1, system.getRestApis().size());
        RestApi restApi = system.getRestApis().getFirst();
        assertSame(restApi, relation.getRestApi());
    }

    @Test
    void importInteraction_alreadyFromGrafanaImported_relationMerged() {

        //given
        String path = "/api/foo/{param}/bar";
        RequestResponseInteractionImporter importer = new RequestResponseInteractionImporter(consumer, provider, "pactUrl");
        RestApi restApi = RestApi.builder()
                .provider(provider)
                .method("GET")
                .path(path)
                .importer(Importer.PACT_BROKER)
                .build();
        provider.getParent().addRestApi(restApi);
        RestApiRelation grafanaRelation = getGrafanaRelation(restApi);
        provider.getParent().addRelation(grafanaRelation);

        assertEquals(1, system.getRelations().size());
        assertTrue(system.getRelations().getFirst().getImporters().contains(Importer.GRAFANA));
        assertFalse(system.getRelations().getFirst().getImporters().contains(Importer.PACT_BROKER));
        assertNull(((RestApiRelation) system.getRelations().getFirst()).getPactUrl());

        RequestResponseInteraction interaction =
                new RequestResponseInteraction("desc", emptyList(), new Request("GET", path));
        RequestResponseInteraction interactionWithRootContextFromPact =
                new RequestResponseInteraction("desc", emptyList(), new Request("GET", "/root-context" + path));

        //when
        importer.importInteraction(interaction);
        importer.importInteraction(interactionWithRootContextFromPact);

        //then
        assertEquals(1, system.getRelations().size());
        RestApiRelation relation = (RestApiRelation) system.getRelations().getFirst();
        assertEquals(provider.getName(), relation.getProviderName());
        assertEquals(consumer.getName(), relation.getConsumerName());
        assertEquals("pactUrl", relation.getPactUrl());
        assertTrue(relation.getImporters().contains(Importer.PACT_BROKER));
        assertTrue(relation.getImporters().contains(Importer.GRAFANA));
        assertEquals("GET", relation.getRestApi().getMethod());
        assertEquals("/api/foo/{param}/bar", relation.getRestApi().getPath());

        assertEquals(1, system.getRestApis().size());
        RestApi createdRestApi = system.getRestApis().getFirst();
        assertSame(createdRestApi, relation.getRestApi());
    }

    @Test
    void importInteraction_alreadyImported_relationMergedWithNewPactUrl() {

        //given
        RequestResponseInteractionImporter importer = new RequestResponseInteractionImporter(consumer, provider, "pactUrl");
        RequestResponseInteraction interaction = new RequestResponseInteraction("desc", emptyList(), new Request("GET", "/foo/123-456-789/bar"));
        importer.importInteraction(interaction);
        assertEquals(1, system.getRelations().size());
        RestApiRelation relation = (RestApiRelation) system.getRelations().getFirst();
        assertEquals(provider.getName(), relation.getProviderName());
        assertEquals(consumer.getName(), relation.getConsumerName());
        assertEquals("pactUrl", relation.getPactUrl());


        //when
        importer = new RequestResponseInteractionImporter(consumer, provider, "pactUrlNew");
        importer.importInteraction(interaction);

        //then
        assertEquals(1, system.getRelations().size());
        relation = (RestApiRelation) system.getRelations().getFirst();
        assertEquals(provider.getName(), relation.getProviderName());
        assertEquals(consumer.getName(), relation.getConsumerName());
        assertEquals("pactUrlNew", relation.getPactUrl());
    }

    @ParameterizedTest
    @CsvSource({
            "/api/internal/externalpermit/permits/410/AX123, /external-permit/api/internal/externalpermit/permits/{restrictionCode}/{permitId}",
            "/foo/123-456-789/bar, /my-service/foo/123-456-789/bar",
            "/foo/{docId}/bar/{uuid}/foo, /foo/{param}/bar/{other}/foo",
            "/foo/{docId}/bar/{uuid}/foo, /input/foo/param/bar/other/foo",
            "/foo/{docId}/bar/{uuid}/foo, /foo/my_param-one/bar/my-param_two/foo",
            "/foo/{docId}/bar/{uuid}/foo, /input/foo/my_param-one/bar/my-param_two/foo",
            "/foo/{docId}/bar/{uuid}/foo, /foo/1234/bar/345-2345-5747345/foo",
            "/foo/{docId}/bar/{uuid}/foo, /input/foo/1234/bar/345-2345-5747345/foo",
            "/api/v1/stations/{stationId}/lane, /traffic-control/api/v1/stations/ch000003-mock-entry-1/lane",
            "/api/v2/systems/{}, /input/api/v2/systems/passar",
            "/api/tariffs/{tnr}, /api/tariffs/9013.2001",
            "/api/v4/businesspartner/with-own-company-id/{companyId}, /api/v4/businesspartner/with-own-company-id/CHE-226.598.037",
            "/api/v2/systems/{system-id}/context-types/{context-type-id}, /input/api/v2/systems/passar/context-types/f6d8a28c-7ec9-44e3-a5b2-a3638070b61f",
            "/api/v4/businesspartner, /api/v4/businesspartner/",
            "/ui-api/v4/businesspartner, /root-context/ui-api/v4/businesspartner",
            "/api/v4/businesspartner, /root-context/api/v4/businesspartner",
            "/api/v4/businesspartner, /root-context/api/v4/businesspartner/",
            "/api/codelists/{codelistName}, /api/codelists/NCL2000"
    })
    void importInteraction_matchingRelationWithOtherAttributeNamesAlreadyImported_relationMergedWithNewPactUrl(String path, String pathFromPact) {

        //given
        RequestResponseInteractionImporter importer = new RequestResponseInteractionImporter(consumer, provider, "pactUrl");
        RequestResponseInteraction interaction = new RequestResponseInteraction("desc", emptyList(), new Request("GET", path));
        importer.importInteraction(interaction);
        assertEquals(1, system.getRelations().size());
        RestApiRelation relation = (RestApiRelation) system.getRelations().getFirst();
        assertEquals(provider.getName(), relation.getProviderName());
        assertEquals(consumer.getName(), relation.getConsumerName());
        assertEquals("pactUrl", relation.getPactUrl());

        //when
        importer = new RequestResponseInteractionImporter(consumer, provider, "pactUrlNew");
        RequestResponseInteraction newInteraction = new RequestResponseInteraction("desc", emptyList(), new Request("GET", pathFromPact));
        importer.importInteraction(newInteraction);

        //then
        assertEquals(1, system.getRelations().size());
        relation = (RestApiRelation) system.getRelations().getFirst();
        assertEquals(provider.getName(), relation.getProviderName());
        assertEquals(consumer.getName(), relation.getConsumerName());
        assertEquals("pactUrlNew", relation.getPactUrl());
        assertEquals(path, relation.getRestApi().getPath());
    }

    @ParameterizedTest
    @CsvSource({
            "/foo/{param}/bar, /foo/123-456-789/bar/foobar",
            "/foo/{param}/bar/foobar/{param}, /foo/123-456-789/bar/foobar",
            "/foo/123-456-789/bar, /foo/123-456-789/bar/foobar",
            "/foo/123-456-789/bar, /my-service/foo/123-456-789/bar/foobar"})
    void importInteraction_noMatchingRelationAlreadyImported_newRelationFromPactAdded(String path, String pathFromPact) {

        //given
        RequestResponseInteractionImporter importer = new RequestResponseInteractionImporter(consumer, provider, "pactUrl");
        RequestResponseInteraction interaction = new RequestResponseInteraction("desc", emptyList(), new Request("GET", path));
        importer.importInteraction(interaction);
        assertEquals(1, system.getRelations().size());
        RestApiRelation relation = (RestApiRelation) system.getRelations().getFirst();
        assertEquals(provider.getName(), relation.getProviderName());
        assertEquals(consumer.getName(), relation.getConsumerName());
        assertEquals("pactUrl", relation.getPactUrl());


        //when
        importer = new RequestResponseInteractionImporter(consumer, provider, "pactUrlNew");
        RequestResponseInteraction newInteraction = new RequestResponseInteraction("desc", emptyList(), new Request("GET", pathFromPact));
        importer.importInteraction(newInteraction);

        //then
        assertEquals(2, system.getRelations().size());
        relation = (RestApiRelation) system.getRelations().getFirst();
        assertEquals(provider.getName(), relation.getProviderName());
        assertEquals(consumer.getName(), relation.getConsumerName());
        if (relation.getRestApi().getPath().equals(path)){
            assertEquals("pactUrl", relation.getPactUrl());
        } else {
            assertEquals("pactUrlNew", relation.getPactUrl());
        }

        relation = (RestApiRelation) system.getRelations().get(1);
        assertEquals(provider.getName(), relation.getProviderName());
        assertEquals(consumer.getName(), relation.getConsumerName());
        if (relation.getRestApi().getPath().equals(path)){
            assertEquals("pactUrl", relation.getPactUrl());
        } else {
            assertEquals("pactUrlNew", relation.getPactUrl());
        }
    }

    private RestApiRelation getGrafanaRelation(RestApi restApi) {
        return RestApiRelation.builder()
                .consumerName(consumer.getName())
                .providerName(provider.getName())
                .importer(Importer.GRAFANA)
                .lastSeen(ZonedDateTime.now())
                .restApi(restApi)
                .build();
    }

    @BeforeEach
    void setup() {
        system = System.builder()
                .name("system")
                .build();
        consumer = BackendService.builder()
                .name("consumer")
                .parent(system)
                .build();
        provider = BackendService.builder()
                .name("provider")
                .parent(system)
                .build();
    }
}
