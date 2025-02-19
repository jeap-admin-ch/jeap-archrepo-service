package ch.admin.bit.jeap.archrepo.importer.prometheus.client;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.PrometheusException;
import ch.admin.bit.jeap.archrepo.importer.prometheus.client.prometheus.dto.PrometheusQueryResponse;
import lombok.experimental.UtilityClass;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class PrometheusHelper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    public static void validateResponse(PrometheusQueryResponse response) {
        if (response == null) {
            throw PrometheusException.responseNotSuccessful();
        }
        if (!"success".equalsIgnoreCase(response.getStatus())) {
            throw PrometheusException.responseNotSuccessful();
        }
        if (!"matrix".equalsIgnoreCase(response.getData().getResultType())) {
            throw PrometheusException.responseNotMatrix();
        }

    }

    public static MultiValueMap<String, String> queryParameters(String queryString, int rangeDays) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
        query.add("query", queryString);
        query.add("start", rfc3339Timestamp(now.minusDays(rangeDays)));
        query.add("end", rfc3339Timestamp(now));
        query.add("step", "1d");
        return query;
    }

    private static String rfc3339Timestamp(ZonedDateTime dateTime) {
        return DATE_TIME_FORMATTER.format(dateTime);
    }
}
