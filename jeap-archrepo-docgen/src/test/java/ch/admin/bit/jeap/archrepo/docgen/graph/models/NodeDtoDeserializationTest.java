package ch.admin.bit.jeap.archrepo.docgen.graph.models;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NodeDtoDeserializationTest {

    private final ObjectMapper objectMapper = new JsonMapper();

    @Test
    void reactionNode_toDot_shouldNotFail_whenJsonHasNoHighlightedOrPartOfClusterFields() {
        String json = """
                {
                  "nodeType": "REACTION",
                  "id": 42,
                  "component": "order-service"
                }
                """;

        ReactionNodeDto node = (ReactionNodeDto) objectMapper.readValue(json, NodeDto.class);

        assertThatCode(node::toDot).doesNotThrowAnyException();
        assertThat(node.toDot())
                .contains("\"REACTION-42\"")
                .doesNotContain("style=filled");
    }

    @Test
    void messageNode_toDot_shouldNotFail_whenJsonHasNoHighlightedField() {
        String json = """
                {
                  "nodeType": "MESSAGE",
                  "id": 7,
                  "messageType": "OrderCreatedEvent"
                }
                """;

        MessageNodeDto node = (MessageNodeDto) objectMapper.readValue(json, NodeDto.class);

        assertThatCode(node::toDot).doesNotThrowAnyException();
        assertThat(node.toDot())
                .contains("\"MESSAGE-7\"")
                .doesNotContain("style=filled");
    }

    @Test
    void graph_toDot_shouldNotFail_whenJsonOmitsBooleanFields() {
        String json = """
                {
                  "nodes": [
                    {"nodeType": "REACTION", "id": 101, "component": "order-service"},
                    {"nodeType": "REACTION", "id": 102, "component": "order-service"},
                    {"nodeType": "MESSAGE", "id": 1, "messageType": "OrderCreatedEvent"}
                  ],
                  "edges": []
                }
                """;

        GraphDto graph = objectMapper.readValue(json, GraphDto.class);

        assertThatCode(graph::toDot).doesNotThrowAnyException();
    }

    @Test
    void reactionNode_toDot_shouldRenderHighlightStyle_whenHighlightedIsTrue() {
        ReactionNodeDto node = new ReactionNodeDto(1, "order-service", true, false);

        assertThat(node.toDot()).contains("style=filled, fillcolor=lightblue");
    }

    @Test
    void reactionNode_toDot_shouldRenderClusterLabel_whenPartOfClusterIsTrue() {
        ReactionNodeDto node = new ReactionNodeDto(1, "order-service", false, true);

        assertThat(node.toDot()).contains("label=\"1\"");
    }

    @Test
    void messageNode_toDot_shouldRenderHighlightStyle_whenHighlightedIsTrue() {
        MessageNodeDto node = new MessageNodeDto(1, "OrderCreatedEvent", null, true);

        assertThat(node.toDot()).contains("style=filled, fillcolor=lightblue");
    }
}
