package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.persistence.MessageGraphRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageGraphServiceTest {

    @Test
    void rendersVariantsInOrderAndHighlightsCurrentMessage() throws Exception {
        MessageType message = mock(MessageType.class);
        when(message.getMessageTypeName()).thenReturn("OrderEvent");
        MessageGraph defaultGraph = graph("", "default".getBytes());
        MessageGraph variantGraph = graph("v1", "variant".getBytes());
        MessageGraphRepository repository = mock(MessageGraphRepository.class);
        when(repository.findAllByMessageTypeName("OrderEvent")).thenReturn(List.of(variantGraph, defaultGraph));

        MessageNodeDto defaultNode = new MessageNodeDto(1, "OrderEvent", null, false);
        MessageNodeDto variantNode = new MessageNodeDto(2, "OrderEvent", "v1", false);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(defaultGraph.getGraphData(), GraphDto.class))
                .thenReturn(new GraphDto(List.of(defaultNode), List.of()));
        when(objectMapper.readValue(variantGraph.getGraphData(), GraphDto.class))
                .thenReturn(new GraphDto(List.of(variantNode), List.of()));

        List<RenderedReactionGraph> result = new MessageGraphService(repository, objectMapper)
                .getGraphs(message, ignored -> "https://confluence/message");

        assertThat(result).extracting(RenderedReactionGraph::title).containsExactly("Default", "v1");
        assertThat(result).allSatisfy(graph -> assertThat(graph.dot()).contains("URL=\"https://confluence/message\""));
        assertThat(defaultNode.getHighlighted()).isTrue();
        assertThat(variantNode.getHighlighted()).isTrue();
    }

    @Test
    void returnsEmptyListWhenNoGraphsExist() {
        MessageType message = mock(MessageType.class);
        when(message.getMessageTypeName()).thenReturn("MissingEvent");
        MessageGraphRepository repository = mock(MessageGraphRepository.class);

        assertThat(new MessageGraphService(repository, mock(ObjectMapper.class)).getGraphs(message, ignored -> null)).isEmpty();
    }

    private MessageGraph graph(String variant, byte[] data) {
        return MessageGraph.builder()
                .messageTypeName("OrderEvent")
                .variant(variant)
                .graphData(data)
                .fingerprint("fingerprint")
                .build();
    }
}
