package ch.admin.bit.jeap.archrepo.importer.reaction;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.GraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.MessageGraphDto;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static ch.admin.bit.jeap.archrepo.test.Pacticipants.ARCHREPO;
import static ch.admin.bit.jeap.archrepo.test.Pacticipants.REACTION_OBSERVER_SERVICE;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("java:S5960") // This is not production code, but a base class for pact tests, we allow assertions
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(port = "8888", pactVersion = PactSpecVersion.V3)
public class ReactionObserverServicePactTestBase {

    private static final String BASE_API_PATH = "/api";
    private static final String GRAPHS_API_PATH = BASE_API_PATH + "/graphs";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("DataFlowIssue")
    @Pact(provider = REACTION_OBSERVER_SERVICE, consumer = ARCHREPO)
    private RequestResponsePact getSystemNamesComponentInteraction(PactDslWithProvider builder) {
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());
        return builder.given("System names are available")
                .uponReceiving("A GET request to " + BASE_API_PATH + "/systems/names")
                .path(BASE_API_PATH + "/systems/names")
                .method("GET")
                .matchHeader("Authorization", "Basic " + basicAuth, "Basic " + basicAuth)
                .willRespondWith()
                .status(200)
                .matchHeader("Content-Type", "application/json")
                .body("""
                        [
                          "system-1",
                          "system-2"
                        ]
                        """)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getSystemNamesComponentInteraction")
    void testGetSystemNames() {
        // given
        ReactionObserverServiceProperties props = new ReactionObserverServiceProperties();
        props.setUrl("http://localhost:8888");
        props.setUsername("user");
        props.setPassword("secret");
        ReactionObserverService reactionObserverService = new ReactionsObserverImporterConfiguration().reactionObserverService(props);

        // when
        List<String> systemNames = reactionObserverService.getSystemNames();

        // then
        assertThat(systemNames).isNotEmpty();
        assertThat(systemNames.getFirst()).isEqualTo("system-1");
        assertThat(systemNames.getLast()).isEqualTo("system-2");
    }

    @SuppressWarnings("DataFlowIssue")
    @Pact(provider = REACTION_OBSERVER_SERVICE, consumer = ARCHREPO)
    private RequestResponsePact getSystemGraphComponentInteraction(PactDslWithProvider builder) {
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());
        return builder.given("System graphs are available")
                .uponReceiving("A GET request to " + GRAPHS_API_PATH + "/systems/sys1")
                .path(GRAPHS_API_PATH + "/systems/sys1")
                .method("GET")
                .matchHeader("Authorization", "Basic " + basicAuth, "Basic " + basicAuth)
                .willRespondWith()
                .status(200)
                .matchHeader("Content-Type", "application/json")
                .body("""
                                {
                                  "graph": {
                                    "nodes": [
                                      {
                                        "nodeType": "MESSAGE",
                                        "id": 2,
                                        "messageType": "Command2",
                                        "variant": null
                                      },
                                      {
                                        "nodeType": "REACTION",
                                        "id": 1,
                                        "component": "service1"
                                      }
                                    ],
                                    "edges": [
                                      {
                                        "edgeType": "TRIGGER",
                                        "sourceId": 2,
                                        "sourceNodeType": "MESSAGE",
                                        "targetReactionId": 1,
                                        "median": 5
                                      }
                                    ]
                                  },
                                  "fingerprint": "d2a3e71875d3419799fd68958990ee058d589becb764607a286d65896ce74de3"
                                }
                        """)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getSystemGraphComponentInteraction")
    void testGetSystemGraph() {
        // given
        ReactionObserverServiceProperties props = new ReactionObserverServiceProperties();
        props.setUrl("http://localhost:8888");
        props.setUsername("user");
        props.setPassword("secret");
        ReactionObserverService reactionObserverService = new ReactionsObserverImporterConfiguration().reactionObserverService(props);

        // when
        GraphDto graph = reactionObserverService.getSystemGraph("sys1");

        // then
        assertThat(graph).isNotNull();
        assertThat(graph.fingerprint()).isEqualTo("d2a3e71875d3419799fd68958990ee058d589becb764607a286d65896ce74de3");

        try {
            String jsonValue = objectMapper.writeValueAsString(graph.graph());
            assertThat(jsonValue).isNotEmpty();

            // JsonPath checks for graph structure
            assertThat(JsonPath.<List<Object>>read(jsonValue, "$.nodes")).hasSize(2);
            assertThat(JsonPath.<String>read(jsonValue, "$.nodes[0].nodeType")).isEqualTo("MESSAGE");
            assertThat(JsonPath.<Integer>read(jsonValue, "$.nodes[0].id")).isEqualTo(2);
            assertThat(JsonPath.<String>read(jsonValue, "$.nodes[0].messageType")).isEqualTo("Command2");

            assertThat(JsonPath.<List<Object>>read(jsonValue, "$.edges")).hasSize(1);
            assertThat(JsonPath.<String>read(jsonValue, "$.edges[0].edgeType")).isEqualTo("TRIGGER");
            assertThat(JsonPath.<Integer>read(jsonValue, "$.edges[0].sourceId")).isEqualTo(2);
            assertThat(JsonPath.<String>read(jsonValue, "$.edges[0].sourceNodeType")).isEqualTo("MESSAGE");
            assertThat(JsonPath.<Integer>read(jsonValue, "$.edges[0].targetReactionId")).isEqualTo(1);
            assertThat(JsonPath.<Integer>read(jsonValue, "$.edges[0].median")).isEqualTo(5);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Pact(provider = REACTION_OBSERVER_SERVICE, consumer = ARCHREPO)
    private RequestResponsePact getComponentGraphComponentInteraction(PactDslWithProvider builder) {
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());
        return builder.given("Component graphs are available")
                .uponReceiving("A GET request to " + GRAPHS_API_PATH + "/components/service1")
                .path(GRAPHS_API_PATH + "/components/service1")
                .method("GET")
                .matchHeader("Authorization", "Basic " + basicAuth, "Basic " + basicAuth)
                .willRespondWith()
                .status(200)
                .matchHeader("Content-Type", "application/json")
                .body("""
                                {
                                  "graph": {
                                    "nodes": [
                                      {
                                        "nodeType": "MESSAGE",
                                        "id": 2,
                                        "messageType": "Command2",
                                        "variant": null
                                      },
                                      {
                                        "nodeType": "REACTION",
                                        "id": 1,
                                        "component": "service1"
                                      }
                                    ],
                                    "edges": [
                                      {
                                        "edgeType": "TRIGGER",
                                        "sourceId": 2,
                                        "sourceNodeType": "MESSAGE",
                                        "targetReactionId": 1,
                                        "median": 5
                                      }
                                    ]
                                  },
                                  "fingerprint": "d2a3e71875d3419799fd68958990ee058d589becb764607a286d65896ce74de3"
                                }
                        """)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getComponentGraphComponentInteraction")
    void testGetComponentGraph() {
        // given
        ReactionObserverServiceProperties props = new ReactionObserverServiceProperties();
        props.setUrl("http://localhost:8888");
        props.setUsername("user");
        props.setPassword("secret");
        ReactionObserverService reactionObserverService = new ReactionsObserverImporterConfiguration().reactionObserverService(props);

        // when
        GraphDto graph = reactionObserverService.getComponentGraph("service1");

        // then
        assertThat(graph).isNotNull();
        assertThat(graph.fingerprint()).isEqualTo("d2a3e71875d3419799fd68958990ee058d589becb764607a286d65896ce74de3");

        try {
            String jsonValue = objectMapper.writeValueAsString(graph.graph());
            assertThat(jsonValue).isNotEmpty();

            // JsonPath checks for graph structure
            assertThat(JsonPath.<List<Object>>read(jsonValue, "$.nodes")).hasSize(2);
            assertThat(JsonPath.<String>read(jsonValue, "$.nodes[0].nodeType")).isEqualTo("MESSAGE");
            assertThat(JsonPath.<Integer>read(jsonValue, "$.nodes[0].id")).isEqualTo(2);
            assertThat(JsonPath.<String>read(jsonValue, "$.nodes[0].messageType")).isEqualTo("Command2");

            assertThat(JsonPath.<List<Object>>read(jsonValue, "$.edges")).hasSize(1);
            assertThat(JsonPath.<String>read(jsonValue, "$.edges[0].edgeType")).isEqualTo("TRIGGER");
            assertThat(JsonPath.<Integer>read(jsonValue, "$.edges[0].sourceId")).isEqualTo(2);
            assertThat(JsonPath.<String>read(jsonValue, "$.edges[0].sourceNodeType")).isEqualTo("MESSAGE");
            assertThat(JsonPath.<Integer>read(jsonValue, "$.edges[0].targetReactionId")).isEqualTo(1);
            assertThat(JsonPath.<Integer>read(jsonValue, "$.edges[0].median")).isEqualTo(5);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Pact(provider = REACTION_OBSERVER_SERVICE, consumer = ARCHREPO)
    private RequestResponsePact getMessageGraphComponentInteraction(PactDslWithProvider builder) {
        String basicAuth = Base64.getEncoder().encodeToString("user:secret".getBytes());
        return builder.given("Message graphs are available")
                .uponReceiving("A GET request to " + GRAPHS_API_PATH + "/messages/ExistingEvent")
                .path(GRAPHS_API_PATH + "/messages/ExistingEvent")
                .method("GET")
                .matchHeader("Authorization", "Basic " + basicAuth, "Basic " + basicAuth)
                .willRespondWith()
                .status(200)
                .matchHeader("Content-Type", "application/json")
                .body("""
                                {
                                  "ExistingEvent/default": {
                                    "graph": {
                                      "nodes": [
                                        {
                                          "nodeType": "MESSAGE",
                                          "id": 123,
                                          "messageType": "ExistingEvent",
                                          "variant": "default"
                                        },
                                        {
                                          "nodeType": "REACTION",
                                          "id": 77,
                                          "component": "notification-service"
                                        }
                                      ],
                                      "edges": [
                                        {
                                          "edgeType": "TRIGGER",
                                          "sourceId": 123,
                                          "sourceNodeType": "MESSAGE",
                                          "targetReactionId": 77,
                                          "median": 10
                                        },
                                        {
                                          "edgeType": "ACTION",
                                          "sourceReactionId": 77,
                                          "targetId": 123,
                                          "targetNodeType": "MESSAGE"
                                        }
                                      ]
                                    },
                                    "fingerprint": "updated-fingerprint"
                                  }
                                }
                        """)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getMessageGraphComponentInteraction")
    void testGetMessageGraph() {
        // given
        ReactionObserverServiceProperties props = new ReactionObserverServiceProperties();
        props.setUrl("http://localhost:8888");
        props.setUsername("user");
        props.setPassword("secret");
        ReactionObserverService reactionObserverService = new ReactionsObserverImporterConfiguration().reactionObserverService(props);

        // when
        MessageGraphDto messageGraph = reactionObserverService.getMessageGraph("ExistingEvent");

        // then
        assertThat(messageGraph).isNotNull();
        assertThat(messageGraph.getVariants()).contains("ExistingEvent/default");

        GraphDto graph = messageGraph.get("ExistingEvent/default");
        assertThat(graph).isNotNull();
        assertThat(graph.fingerprint()).isEqualTo("updated-fingerprint");

        try {
            String jsonValue = objectMapper.writeValueAsString(graph.graph());
            assertThat(jsonValue).isNotEmpty();

            // Check that there are exactly 2 nodes
            assertThat(JsonPath.<List<Object>>read(jsonValue, "$.nodes")).hasSize(2);

            // Find the MESSAGE node with id 123
            List<Map<String, Object>> messageNodes = JsonPath.read(jsonValue, "$.nodes[?(@.nodeType == 'MESSAGE' && @.id == 123)]");
            assertThat(messageNodes).hasSize(1);
            assertThat(messageNodes.getFirst().get("variant")).isEqualTo("default");
            assertThat(messageNodes.getFirst().get("messageType")).isEqualTo("ExistingEvent");

            // Find the REACTION node with component 'notification-service'
            List<Map<String, Object>> reactionNodes = JsonPath.read(jsonValue, "$.nodes[?(@.nodeType == 'REACTION' && @.component == 'notification-service')]");
            assertThat(reactionNodes).hasSize(1);

            // Check that there are exactly 2 edges
            assertThat(JsonPath.<List<Object>>read(jsonValue, "$.edges")).hasSize(2);

            // Find the TRIGGER edge with specific source and target
            List<Map<String, Object>> triggerEdges = JsonPath.read(jsonValue, "$.edges[?(@.edgeType == 'TRIGGER' && @.sourceId == 123 && @.targetReactionId == 77)]");
            assertThat(triggerEdges).hasSize(1);
            assertThat(triggerEdges.getFirst().get("sourceNodeType")).isEqualTo("MESSAGE");
            assertThat(triggerEdges.getFirst().get("median")).isEqualTo(10);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
