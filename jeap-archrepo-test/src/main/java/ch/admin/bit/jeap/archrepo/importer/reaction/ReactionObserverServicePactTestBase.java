package ch.admin.bit.jeap.archrepo.importer.reaction;

import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.Action;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionsObservedStatisticsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Base64;
import java.util.List;

import static ch.admin.bit.jeap.archrepo.test.Pacticipants.ARCHREPO;
import static ch.admin.bit.jeap.archrepo.test.Pacticipants.REACTION_OBSERVER_SERVICE;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("java:S5960") // This is not production code, but a base class for pact tests, we allow assertions
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(port = "8888", pactVersion = PactSpecVersion.V3)
public class ReactionObserverServicePactTestBase {

    private static final String API_PATH = "/api/statisticsV2";

    @SuppressWarnings("DataFlowIssue")
    @Pact(provider = REACTION_OBSERVER_SERVICE, consumer = ARCHREPO)
    private RequestResponsePact getStatisticsForKnownComponentInteraction(PactDslWithProvider builder) {
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());
        return builder.given("Statistics for component 'c1' are available")
                .uponReceiving("A GET request to " + API_PATH + "/c1")
                .path(API_PATH + "/c1")
                .method("GET")
                .matchHeader("Authorization", "Basic " + basicAuth, "Basic " + basicAuth)
                .willRespondWith()
                .status(200)
                .matchHeader("Content-Type", "application/json")
                .body("""
                [
                  {
                    "component": "c1",
                    "triggerType": "command",
                    "triggerFqn": "SomeCommand",
                    "actions": [
                      {
                        "actionType": "event",
                        "actionFqn": "SomeEvent",
                        "actionProperties": {}
                      }
                    ],
                    "count": 100,
                    "median": 50.0,
                    "percentage": 75.0,
                    "triggerProperties": {}
                  }
                ]
                """)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getStatisticsForKnownComponentInteraction")
    void testGetStatistics() {
        // given
        ReactionObserverServiceProperties props = new ReactionObserverServiceProperties();
        props.setUrl("http://localhost:8888");
        props.setUsername("user");
        props.setPassword("secret");
        ReactionObserverService reactionObserverService = new ReactionsObserverImporterConfiguration().reactionObserverService(props);

        // when
        List<ReactionsObservedStatisticsDto> result = reactionObserverService.getReactionsObservedStatistics("c1");

        // then
        assertThat(result).isNotEmpty();
        ReactionsObservedStatisticsDto statisticsDto = result.getFirst();
        assertThat(statisticsDto.component()).isEqualTo("c1");
        assertThat(statisticsDto.triggerType()).isEqualTo("command");
        assertThat(statisticsDto.triggerFqn()).isEqualTo("SomeCommand");
        assertThat(statisticsDto.actions()).isNotEmpty();
        Action action = statisticsDto.actions().getFirst();
        assertThat(action.actionType()).isEqualTo("event");
        assertThat(action.actionFqn()).isEqualTo("SomeEvent");
        assertThat(statisticsDto.count()).isGreaterThan(0);
        assertThat(statisticsDto.median()).isGreaterThan(0d);
        assertThat(statisticsDto.percentage()).isGreaterThan(0d);
    }

    @SuppressWarnings("DataFlowIssue")
    @Pact(provider = REACTION_OBSERVER_SERVICE, consumer = ARCHREPO)
    private RequestResponsePact getStatisticsForUnknownComponentInteraction(PactDslWithProvider builder) {
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());
        return builder.given("Statistics for unknown component are empty")
                .uponReceiving("A GET request to " + API_PATH + "/unknown")
                .path(API_PATH + "/unknown")
                .method("GET")
                .matchHeader("Authorization", "Basic " + basicAuth, "Basic " + basicAuth)
                .willRespondWith()
                .status(200)
                .matchHeader("Content-Type", "application/json")
                .body(PactDslJsonArray.newUnorderedArray())
                .toPact();
    }


    @Test
    @PactTestFor(pactMethod = "getStatisticsForUnknownComponentInteraction")
    void testGetStatisticsUnknownComponent() {
        // given
        ReactionObserverServiceProperties props = new ReactionObserverServiceProperties();
        props.setUrl("http://localhost:8888");
        props.setUsername("user");
        props.setPassword("secret");
        ReactionObserverService reactionObserverService = new ReactionsObserverImporterConfiguration().reactionObserverService(props);

        // when
        List<ReactionsObservedStatisticsDto> result = reactionObserverService.getReactionsObservedStatistics("unknown");

        // then
        assertThat(result).isEmpty();
    }
}
