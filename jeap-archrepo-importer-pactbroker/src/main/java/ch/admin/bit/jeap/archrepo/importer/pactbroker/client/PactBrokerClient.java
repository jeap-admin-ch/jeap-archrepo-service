package ch.admin.bit.jeap.archrepo.importer.pactbroker.client;

import au.com.dius.pact.core.model.*;
import au.com.dius.pact.core.pactbroker.HalClient;
import au.com.dius.pact.core.pactbroker.PactBrokerClientConfig;
import au.com.dius.pact.core.support.json.JsonValue;
import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Slf4j
public class PactBrokerClient {

    private static final String LATEST_PACTS_LINK_NAME = "pb:latest-pact-versions";
    private static final String PACTS_LINK_NAME = "pb:pacts";
    private static final String HREF_ATTRIBUTE_NAME = "href";

    private final CloseableHttpClient httpClient;
    private final String pactBrokerUrl;

    public void withAllPacts(Consumer<BrokeredPact> pactCallback) {
        List<String> latestPactUrls = getLatestPactUrls();

        latestPactUrls.forEach(pactUrl ->
                loadPactInvokingCallbackOnSuccess(pactCallback, pactUrl));
    }

    private void loadPactInvokingCallbackOnSuccess(Consumer<BrokeredPact> pactCallback, String pactUrl) {
        try {
            BrokeredPact brokeredPact = BrokeredPact.builder()
                    .pactUrl(pactUrl)
                    .pact(loadPact(pactUrl))
                    .build();
            pactCallback.accept(brokeredPact);
        } catch (Exception ex) {
            log.warn("Failed to load pact from URL {}", pactUrl, ex);
        }
    }

    private List<String> getLatestPactUrls() {
        HalClient halClient = new HalClient(pactBrokerUrl, new PactBrokerClientConfig());
        List<String> pactUrls = new ArrayList<>();
        halClient.navigate(LATEST_PACTS_LINK_NAME).forAll(PACTS_LINK_NAME, map -> {
            String pactUrl = (String) map.get(HREF_ATTRIBUTE_NAME);
            pactUrls.add(pactUrl);
        });
        return pactUrls;
    }

    private Pact loadPact(String url) {
        BrokerUrlSource urlSource = new BrokerUrlSource(url, pactBrokerUrl);
        JsonValue.Object pactJson = downloadPact(urlSource);
        return parsePact(urlSource, pactJson);
    }

    private JsonValue.Object downloadPact(BrokerUrlSource urlSource) {
        Pair<JsonValue.Object, PactSource> jsonElementPactSourcePair =
                PactReaderKt.loadPactFromUrl(urlSource, Map.of(), httpClient);
        return jsonElementPactSourcePair.getFirst();
    }

    private Pact parsePact(BrokerUrlSource urlSource, JsonValue.Object pactJson) {
        return DefaultPactReader.loadV3Pact(urlSource, pactJson);
    }
}
