package ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.aws.client.prometheus.AWSPrometheusProxy;
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
class AWSPrometheusClientTest {

    @InjectMocks
    private AWSPrometheusClient awsPrometheusClient;

    @Mock
    private AWSPrometheusProxy awsPrometheusProxy;

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
        when(awsPrometheusProxy.queryRange(eq("jeap_relation_total{stage=\"ref\"}"), anyInt())).thenReturn(resultRef);
        when(awsPrometheusProxy.queryRange(eq("jeap_relation_total{stage=\"abn\"}"), anyInt())).thenReturn(resultAbn);
        when(awsPrometheusProxy.queryRange(eq("jeap_relation_total{stage=\"prod\"}"), anyInt())).thenReturn(resultProd);

        //when
        Collection<JeapRelation> jeapRelationsOnRef = awsPrometheusClient.apiRelations("ref");
        Collection<JeapRelation> jeapRelationsOnAbn = awsPrometheusClient.apiRelations("abn");
        Collection<JeapRelation> jeapRelationsOnProd = awsPrometheusClient.apiRelations("prod");

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
