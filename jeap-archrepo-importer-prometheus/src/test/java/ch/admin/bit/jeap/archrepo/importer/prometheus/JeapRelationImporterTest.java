package ch.admin.bit.jeap.archrepo.importer.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.JeapRelation;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JeapRelationImporterTest {

    private System system;

    private BackendService consumer;

    private BackendService provider;


    @Test
    void importRelation_oneNewRelation_relationImported() {

        //given
        JeapRelationImporter jeapRelationImporter = new JeapRelationImporter(consumer, provider);
        assertTrue(system.getRelations().isEmpty());
        assertTrue(system.getRestApis().isEmpty());

        //when
        JeapRelation jeapRelation = getJeapRelation(provider.getName(), consumer.getName());
        jeapRelationImporter.importRelation(jeapRelation);

        //then
        assertEquals(1, system.getRelations().size());
        RestApiRelation relation = (RestApiRelation) system.getRelations().getFirst();
        assertEquals(jeapRelation.getProvider(), provider.getName());
        assertEquals(provider.getName(), relation.getProviderName());
        assertEquals(jeapRelation.getConsumer(), consumer.getName());
        assertEquals(consumer.getName(), relation.getConsumerName());
        assertNull(relation.getPactUrl());
        assertTrue(relation.getImporters().contains(Importer.GRAFANA));
        assertEquals(jeapRelation.getMethod(), relation.getRestApi().getMethod());
        assertEquals(jeapRelation.getDatapoint(), relation.getRestApi().getPath());

        assertEquals(1, system.getRestApis().size());
        RestApi restApi = system.getRestApis().getFirst();
        assertSame(restApi, relation.getRestApi());
    }

    @Test
    void importRelation_relationAlreadyPresent_relationNotImported() {

        //given
        JeapRelationImporter jeapRelationImporter = new JeapRelationImporter(consumer, provider);
        JeapRelation jeapRelation = getJeapRelation(provider.getName(), consumer.getName());
        jeapRelationImporter.importRelation(jeapRelation);
        assertEquals(1, system.getRelations().size());
        assertEquals(1, system.getRestApis().size());

        //when
        jeapRelationImporter.importRelation(jeapRelation);

        //then
        assertEquals(1, system.getRelations().size());
        assertEquals(1, system.getRestApis().size());
        assertEquals(1, system.getRestApis().size());

    }

    @ParameterizedTest
    @CsvSource({
            "/api/codelists/specific, /api/codelists/{generic}"
    })
    void getRestApiForPathVariablePath_doNotMatchGenericEndpointToSpecificOne() {
        JeapRelationImporter jeapRelationImporter = new JeapRelationImporter(consumer, provider);

        RestApi specificRestApi = getRestApi(provider, "/api/specific");
        provider.getParent().addRestApi(specificRestApi);

        Optional<RestApi> foundRestApi = jeapRelationImporter.getRestApi(provider, "GET", "/api/{something}");
        assertFalse(foundRestApi.isPresent());
    }

    @Test
    void getRestApiForPathVariablePath_returnsPathVariableRestApi() {
        JeapRelationImporter jeapRelationImporter = new JeapRelationImporter(consumer, provider);

        RestApi specificRestApi = getRestApi(provider, "/api/specific");
        RestApi genericRestApi = getRestApi(provider, "/api/{value}");
        provider.getParent().addRestApi(specificRestApi);
        provider.getParent().addRestApi(genericRestApi);

        Optional<RestApi> foundRestApi = jeapRelationImporter.getRestApi(provider, "GET", "/api/{something}");
        assertTrue(foundRestApi.isPresent());
        assertEquals(genericRestApi, foundRestApi.get());
    }

    @Test
    void importRelation_relationWithGenericPathAlreadyPresent_onlyGenericPathRelationIsUpdated() {

        //given
        JeapRelationImporter jeapRelationImporter = new JeapRelationImporter(consumer, provider);
        JeapRelation jeapRelation = new JeapRelation(provider.getName(), consumer.getName(), "GET", "http", "/api/{something}");
        ZonedDateTime lastSeenTime = ZonedDateTime.of(2024, 6, 11, 10, 10, 0, 0, ZoneId.systemDefault());
        RestApiRelation specificRestApiRelation = getGrafanaRelation("/api/specific", lastSeenTime);
        RestApiRelation genericRestApiRelation = getGrafanaRelation("/api/{value}", lastSeenTime);
        provider.getParent().addRelation(specificRestApiRelation);
        provider.getParent().addRestApi(specificRestApiRelation.getRestApi());
        provider.getParent().addRelation(genericRestApiRelation);
        provider.getParent().addRestApi(genericRestApiRelation.getRestApi());
        assertEquals(2, system.getRelations().size());
        assertEquals(2, system.getRestApis().size());

        //when
        jeapRelationImporter.importRelation(jeapRelation);

        //then
        assertEquals(lastSeenTime, ((RestApiRelation)system.getRelations().get(0)).getLastSeen());
        assertNotEquals(lastSeenTime, ((RestApiRelation)system.getRelations().get(1)).getLastSeen());
        assertEquals(2, system.getRestApis().size());
    }

    @Test
    void importRelation_relationFromPactWithOtherPathPresent_relationImported() {

        //given
        JeapRelationImporter jeapRelationImporter = new JeapRelationImporter(consumer, provider);
        RestApiRelation pactRelation = getPactRelation("/api/other");
        provider.getParent().addRelation(pactRelation);
        provider.getParent().addRestApi(pactRelation.getRestApi());

        assertEquals(1, system.getRelations().size());
        assertEquals(1, system.getRestApis().size());

        //when
        JeapRelation jeapRelation = getJeapRelation(provider.getName(), consumer.getName());
        jeapRelationImporter.importRelation(jeapRelation);

        //then
        assertEquals(2, system.getRelations().size());
        assertEquals(2, system.getRestApis().size());
        assertTrue(system.getRelations().get(0).getImporters().contains(Importer.PACT_BROKER));
        assertTrue(system.getRelations().get(1).getImporters().contains(Importer.GRAFANA));

    }

    @Test
    void importRelation_relationFromPactWithSamePathPresent_relationMerged() {

        //given
        JeapRelationImporter jeapRelationImporter = new JeapRelationImporter(consumer, provider);
        RestApiRelation pactRelation = getPactRelation("/api/test");
        provider.getParent().addRestApi(pactRelation.getRestApi());
        provider.getParent().addRelation(pactRelation);

        assertEquals(1, system.getRelations().size());
        assertTrue(system.getRelations().getFirst().getImporters().contains(Importer.PACT_BROKER));
        assertFalse(system.getRelations().getFirst().getImporters().contains(Importer.GRAFANA));
        assertEquals(1, system.getRestApis().size());

        //when
        JeapRelation jeapRelation = getJeapRelation(provider.getName(), consumer.getName());
        jeapRelationImporter.importRelation(jeapRelation);

        //then
        assertEquals(1, system.getRelations().size());
        assertTrue(system.getRelations().getFirst().getImporters().contains(Importer.PACT_BROKER));
        assertTrue(system.getRelations().getFirst().getImporters().contains(Importer.GRAFANA));
        assertEquals(1, system.getRestApis().size());
        assertEquals(1, system.getRestApis().size());
        assertEquals(pactRelation.getPactUrl(), ((RestApiRelation) system.getRelations().getFirst()).getPactUrl());

    }


    private JeapRelation getJeapRelation(String provider, String consumer) {
        return new JeapRelation(provider, consumer, "GET", "http", "/api/test");
    }

    private RestApiRelation getPactRelation(String path) {
        RestApi restApi = RestApi.builder()
                .provider(provider)
                .method("GET")
                .path(path)
                .importer(Importer.PACT_BROKER)
                .build();
        return RestApiRelation.builder()
                .consumerName(consumer.getName())
                .providerName(provider.getName())
                .pactUrl("http://pacturl.test")
                .restApi(restApi)
                .importer(Importer.PACT_BROKER)
                .lastSeen(ZonedDateTime.now())
                .build();
    }

    private RestApiRelation getGrafanaRelation(String path, ZonedDateTime lastSeenTime) {
        RestApi restApi = RestApi.builder()
                .provider(provider)
                .method("GET")
                .path(path)
                .importer(Importer.GRAFANA)
                .build();
        return RestApiRelation.builder()
                .consumerName(consumer.getName())
                .providerName(provider.getName())
                .restApi(restApi)
                .importer(Importer.GRAFANA)
                .lastSeen(lastSeenTime)
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

    private RestApi getRestApi(SystemComponent systemComponent, String path) {
        return RestApi.builder()
                .provider(systemComponent)
                .method("GET")
                .path(path)
                .importer(Importer.GRAFANA)
                .build();
    }

}
