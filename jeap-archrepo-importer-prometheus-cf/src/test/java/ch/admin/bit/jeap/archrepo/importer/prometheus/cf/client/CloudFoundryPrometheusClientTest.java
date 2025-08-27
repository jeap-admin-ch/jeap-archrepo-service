package ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.cf.client.prometheus.CloudFoundryPrometheusProxy;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.JeapRelation;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponseResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudFoundryPrometheusClientTest {

    @InjectMocks
    private CloudFoundryPrometheusClient cloudFoundryPrometheusClient;

    @Mock
    private CloudFoundryPrometheusProxy cloudFoundryPrometheusProxy;

    @Test
    void apiRelations_relationsFound_returnList() {

        //given
        List<PrometheusQueryResponseResult> resultRef = List.of(
                createPrometheusResult("/ref"));
        List<PrometheusQueryResponseResult> resultAbn = List.of(
                createPrometheusResult("/abnFoo"),
                createPrometheusResult("/abn"),
                createPrometheusResult("/abnAndProd"));
        List<PrometheusQueryResponseResult> resultProd = List.of(
                createPrometheusResult("/abnAndProd"),
                createPrometheusResult("/prod"));
        when(cloudFoundryPrometheusProxy.queryRange(eq("jeap_relation_total{space_name=\"ref\"}"), anyInt())).thenReturn(resultRef);
        when(cloudFoundryPrometheusProxy.queryRange(eq("jeap_relation_total{space_name=\"abn\"}"), anyInt())).thenReturn(resultAbn);
        when(cloudFoundryPrometheusProxy.queryRange(eq("jeap_relation_total{space_name=\"prod\"}"), anyInt())).thenReturn(resultProd);

        //when
        Collection<JeapRelation> jeapRelationsOnRef = cloudFoundryPrometheusClient.apiRelations("ref");
        Collection<JeapRelation> jeapRelationsOnAbn = cloudFoundryPrometheusClient.apiRelations("abn");
        Collection<JeapRelation> jeapRelationsOnProd = cloudFoundryPrometheusClient.apiRelations("prod");

        //then
        assertEquals(1, jeapRelationsOnRef.size());
        assertEquals(3, jeapRelationsOnAbn.size());
        assertEquals(2, jeapRelationsOnProd.size());
    }

    private static PrometheusQueryResponseResult createPrometheusResult(String datapoint) {
        Map<String, String> metrics = Map.of(
                "datapoint", datapoint,
                "producer", "test",
                "consumer", "test",
                "method", "POST"
        );
        return new PrometheusQueryResponseResult(metrics);
    }
}
