package ch.admin.bit.jeap.archrepo.docgen.graph;

import ch.admin.bit.jeap.archrepo.docgen.graph.models.GraphDto;
import ch.admin.bit.jeap.archrepo.docgen.graph.models.MessageNodeDto;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemGraph;
import ch.admin.bit.jeap.archrepo.persistence.SystemGraphRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemGraphServiceTest {

    @Test
    void rendersLinkedDotAndHighlightsMessagesFromOtherSystems() throws Exception {
        System system = mock(System.class);
        when(system.getName()).thenReturn("MySystem");
        SystemGraph graph = mock(SystemGraph.class);
        byte[] graphData = "graph".getBytes();
        when(graph.getGraphData()).thenReturn(graphData);

        SystemGraphRepository repository = mock(SystemGraphRepository.class);
        when(repository.findBySystemNameIgnoreCase("MySystem")).thenReturn(graph);
        MessageNodeDto ownMessage = new MessageNodeDto(1, "MySystemEvent", null, false);
        MessageNodeDto otherMessage = new MessageNodeDto(2, "OtherSystemEvent", null, false);
        GraphDto graphDto = new GraphDto(List.of(ownMessage, otherMessage), List.of());
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.readValue(graphData, GraphDto.class)).thenReturn(graphDto);

        RenderedReactionGraph result = new SystemGraphService(repository, objectMapper)
                .getGraph(system, ignored -> "https://confluence/page");

        assertThat(result.dot()).contains("URL=\"https://confluence/page\"", "target=\"_top\"");
        assertThat(ownMessage.getHighlighted()).isFalse();
        assertThat(otherMessage.getHighlighted()).isTrue();
    }

    @Test
    void returnsNullWhenNoGraphExists() {
        System system = mock(System.class);
        when(system.getName()).thenReturn("Missing");
        SystemGraphRepository repository = mock(SystemGraphRepository.class);

        assertThat(new SystemGraphService(repository, mock(ObjectMapper.class)).getGraph(system, ignored -> null)).isNull();
    }
}
