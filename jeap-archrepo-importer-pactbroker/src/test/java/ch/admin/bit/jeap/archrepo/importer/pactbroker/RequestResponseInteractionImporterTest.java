package ch.admin.bit.jeap.archrepo.importer.pactbroker;

import au.com.dius.pact.core.model.Request;
import au.com.dius.pact.core.model.RequestResponseInteraction;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.Relation;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZonedDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void importInteraction_multipleMatchingPathsAlreadyFromGrafanaImported_relationMergedWithMoreSpecific() {

        //given
        String pathUnspecific1 = "/api/storage/docs/{docId}/files/{mimeId}";
        String pathUnspecific2 = "/api/storage/docs/{docId}/{var}/{mimeId}";
        String pathSpecific = "/api/storage/docs/{docId}/files/filename";
        String pathUnspecific3 = "/api/storage/docs/{docId}/files/filename/{extra}";

        createGrafanaRelation(pathUnspecific1);
        createGrafanaRelation(pathUnspecific2);
        createGrafanaRelation(pathSpecific);
        createGrafanaRelation(pathUnspecific3);

        assertEquals(4, system.getRelations().size());

        system.getRelations().forEach(relation -> {
            assertTrue(relation.getImporters().contains(Importer.GRAFANA));
            assertFalse(relation.getImporters().contains(Importer.PACT_BROKER));
            assertNull(((RestApiRelation) relation).getPactUrl());
        });

        RequestResponseInteractionImporter importer = new RequestResponseInteractionImporter(consumer, provider, "pactUrl");
        RequestResponseInteraction interaction =
                new RequestResponseInteraction("desc", emptyList(), new Request("GET", "/docbox-storage-service/api/storage/docs/251107075724354730/files/filename"));

        //when
        importer.importInteraction(interaction);

        //then
        assertEquals(4, system.getRelations().size());
        RestApiRelation relation = getRelationWithPath(system.getRelations(), pathSpecific);
        assertEquals(provider.getName(), relation.getProviderName());
        assertEquals(consumer.getName(), relation.getConsumerName());
        assertEquals("pactUrl", relation.getPactUrl());
        assertTrue(relation.getImporters().contains(Importer.PACT_BROKER));
        assertTrue(relation.getImporters().contains(Importer.GRAFANA));
        assertEquals("GET", relation.getRestApi().getMethod());
    }


    @ParameterizedTest
    @CsvSource({
            // Exact matches
            "/api/resource, /api/resource, true",
            "/api/resource, /api/resources, false",
            // Path variable matching - pact path matches JEAP pattern
            "/api/users/{userId}, /api/users/123, true",
            "/api/users/{userId}/posts, /api/users/456/posts, true",
            "/api/users/{userId}/posts/{postId}, /api/users/123/posts/456, true",
            // Path variable matching - JEAP pattern matches pact path
            "/api/orders/123, /api/orders/{orderId}, true",
            "/api/products/abc-123, /api/products/{productId}, true",
            // Multiple path variables with different names
            "/api/users/{userId}/posts/{postId}, /api/users/id/posts/pid, true",
            "/api/shops/{shopId}/items/{itemId}, /api/shops/shop-1/items/item-2, true",
            // Context prefix handling
            "/context/api/resource, /api/resource, true",
            "/my-service/api/users/{id}, /api/users/123, true",
            "/api/resource, /context/api/resource, true",
            // Trailing slash handling
            "/api/resource, /api/resource/, true",
            "/api/resource/, /api/resource, true",
            "/api/users/{id}, /api/users/123/, true",
            // Complex paths with UUID-like patterns
            "/api/docs/{docId}, /api/docs/251107075724354730, true",
            "/api/files/{fileId}, /api/files/f6d8a28c-7ec9-44e3-a5b2-a3638070b61f, true",
            // Alphanumeric and special characters
            "/api/permits/{permitId}, /api/permits/AX123, true",
            "/api/stations/{stationId}, /api/stations/ch000003-mock-entry-1, true",
            "/api/tariffs/{tnr}, /api/tariffs/9013.2001, true",
            "/api/businesspartner/{companyId}, /api/businesspartner/CHE-226.598.037, true",
            // Segregated APIs (ui-api, etc.)
            "/ui-api/resource, /ui-api/resource, true",
            "/ui-api/users/{id}, /ui-api/users/123, true",
            "/context/ui-api/resource, /ui-api/resource, true",
            // Non-matching cases
            "/api/users/{id}, /api/posts/{id}, false",
            "/api/users/{id}/posts, /api/users/{id}, false",
            "/api/v1/resource, /api/v2/resource, false",
            "/api/users, /api/users/{id}/posts, false"
    })
    void retrieveExistingRestApiWithMatchingPattern_variousPaths_matchesCorrectly(String jeapPath, String pactPath, boolean shouldMatch) {
        // given
        RestApi restApi = RestApi.builder()
                .provider(provider)
                .method("GET")
                .path(jeapPath)
                .importer(Importer.GRAFANA)
                .build();
        system.addRestApi(restApi);

        RequestResponseInteractionImporter importer = new RequestResponseInteractionImporter(consumer, provider, "pactUrl");

        // when
        var result = importer.retrieveExistingRestApiWithMatchingPattern(provider, "GET", pactPath);

        // then
        if (shouldMatch) {
            assertTrue(result.isPresent(), "Expected to find matching REST API for JEAP path '" + jeapPath + "' and Pact path '" + pactPath + "'");
            assertEquals(jeapPath, result.get().getPath());
        } else {
            assertFalse(result.isPresent(), "Expected no match for JEAP path '" + jeapPath + "' and Pact path '" + pactPath + "'");
        }
    }


    private RestApiRelation getRelationWithPath(List<Relation> relations, String path) {
        List<Relation> list = relations.stream()
                .filter(r -> ((RestApiRelation) r).getRestApi().getPath().equals(path)).toList();
        assertThat(list).hasSize(1);
        return (RestApiRelation) list.getFirst();
    }

    private void createGrafanaRelation(String path) {
        RestApi restApi = RestApi.builder()
                .provider(provider)
                .method("GET")
                .path(path)
                .importer(Importer.GRAFANA)
                .build();
        provider.getParent().addRestApi(restApi);
        RestApiRelation grafanaRelation = getGrafanaRelation(restApi);
        provider.getParent().addRelation(grafanaRelation);

        RestApiRelation.builder()
                .consumerName(consumer.getName())
                .providerName(provider.getName())
                .importer(Importer.GRAFANA)
                .lastSeen(ZonedDateTime.now())
                .restApi(restApi)
                .build();
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
