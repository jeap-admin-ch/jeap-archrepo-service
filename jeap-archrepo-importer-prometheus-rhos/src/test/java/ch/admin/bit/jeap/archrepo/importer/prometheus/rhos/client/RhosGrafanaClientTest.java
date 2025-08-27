package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.JeapRelation;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.RhosGrafanaAccess;
import ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client.prometheus.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RhosGrafanaClientTest {

    @InjectMocks
    private RhosGrafanaClient grafanaClient;

    @Mock
    private RhosGrafanaAccess grafanaAccess;

    @Test
    void apiRelations_relationsFound_returnList() {
        //given
        List<RhosGrafanaQueryResponseData> resultRef = List.of(
                createJeapRelationResult("/ref"));
        List<RhosGrafanaQueryResponseData>  resultAbn = List.of(
                createJeapRelationResult("/abnFoo"),
                createJeapRelationResult("/abn"),
                createJeapRelationResult("/abnAndProd"));
        List<RhosGrafanaQueryResponseData>  resultProd = List.of(
                createJeapRelationResult("/abnAndProd"),
                createJeapRelationResult("/prod"));
        when(grafanaAccess.queryRange(eq("jeap_relation_total"), eq("r"), anyInt())).thenReturn(resultRef);
        when(grafanaAccess.queryRange(eq("jeap_relation_total"), eq("a"), anyInt())).thenReturn(resultAbn);
        when(grafanaAccess.queryRange(eq("jeap_relation_total"), eq("p"), anyInt())).thenReturn(resultProd);

        //when
        Collection<JeapRelation> jeapRelationsOnRef = grafanaClient.apiRelations("ref");
        Collection<JeapRelation> jeapRelationsOnAbn = grafanaClient.apiRelations("abn");
        Collection<JeapRelation> jeapRelationsOnProd = grafanaClient.apiRelations("prod");

        //then
        assertEquals(1, jeapRelationsOnRef.size());
        assertEquals(3, jeapRelationsOnAbn.size());
        assertEquals(2, jeapRelationsOnProd.size());
    }

    @Test
    void services_relationsFound_returnList() {
        //given
        List<RhosGrafanaQueryResponseData> resultRef = List.of(
                createServiceResult("my_service1"),
                createServiceResult("my_service2"),
                createServiceResult("my_service3")
                );
        when(grafanaAccess.queryRange(eq("group by(name) (jeap_spring_app{namespace=\\\"bit-jme-d\\\"})"), eq("r"), anyInt())).thenReturn(resultRef);

        //when
        Set<String> services = grafanaClient.services("ref", "bit-jme-d");

        //then
        assertEquals(3, services.size());
    }

    private static RhosGrafanaQueryResponseData createJeapRelationResult(String datapoint) {
        Map<String, String> labels = Map.of(
                "datapoint", datapoint,
                "producer", "test",
                "consumer", "test",
                "method", "POST"
        );
        return prepareResponseData(labels);
    }

    private static RhosGrafanaQueryResponseData createServiceResult(String service) {
        Map<String, String> labels = Map.of(
                "name", service
        );
        return prepareResponseData(labels);
    }

    private static RhosGrafanaQueryResponseData prepareResponseData(Map<String, String> labels) {
        RhosGrafanaSchema schema = new RhosGrafanaSchema();
        RhosGrafanaField timeField = new RhosGrafanaField();
        timeField.setName("Time");
        RhosGrafanaField metricField = new RhosGrafanaField();
        metricField.setName("jeap_spring_app");
        metricField.setLabels(labels);
        schema.setFields(List.of(timeField, metricField));
        RhosGrafanaFrame frame = new RhosGrafanaFrame();

        frame.setSchema(schema);
        RhosGrafanaQueryResult result = new RhosGrafanaQueryResult();
        result.setStatus(200);

        result.setFrames(List.of(frame));

        RhosGrafanaQueryResponseData responseData = new RhosGrafanaQueryResponseData();
        responseData.setResults(Map.of("A", result));
        return responseData;
    }
}
