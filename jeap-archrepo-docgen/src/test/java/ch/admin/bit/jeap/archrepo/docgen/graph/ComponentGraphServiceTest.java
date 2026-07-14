package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.system.ComponentGraph;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.ComponentGraphRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComponentGraphServiceTest {

    @Test
    void rendersLinkedDotAndHighlightsMessagesFromOtherSystems() throws Exception {
        SystemComponent component = mock(SystemComponent.class);
        when(component.getName()).thenReturn("orders-service");
        ComponentGraph graph = mock(ComponentGraph.class);
        when(graph.getSystemName()).thenReturn("Orders");
        byte[] graphData = "graph".getBytes();
        when(graph.getGraphData()).thenReturn(graphData);

        ComponentGraphRepository repository = mock(ComponentGraphRepository.class);
        when(repository.findByComponentNameIgnoreCase("orders-service")).thenReturn(graph);
        MessageNodeDto message = new MessageNodeDto(1, "OtherEvent", null, false);
        GraphDto graphDto = new GraphDto(List.of(message), List.of());
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(graphData, GraphDto.class)).thenReturn(graphDto);

        RenderedReactionGraph result = new ComponentGraphService(repository, objectMapper)
                .getGraph(component, ignored -> "https://confluence/page");

        assertThat(result.dot()).contains("URL=\"https://confluence/page\"");
        assertThat(message.getHighlighted()).isTrue();
    }
}
