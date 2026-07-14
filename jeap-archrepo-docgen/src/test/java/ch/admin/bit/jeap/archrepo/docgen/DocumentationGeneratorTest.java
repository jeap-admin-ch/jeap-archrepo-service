package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.graph.ComponentGraphService;
import ch.admin.bit.jeap.archrepo.docgen.graph.MessageGraphService;
import ch.admin.bit.jeap.archrepo.docgen.graph.SystemGraphService;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentationGeneratorTest {

    private static final String ROOT_PAGE_ID = "rootPageId";
    private static final String SYSTEM_NAME = "MockSystem";

    @Mock
    private ConfluenceAdapter confluenceAdapter;

    @Mock
    private MessageGraphService messageGraphService;

    @Mock
    private SystemGraphService systemGraphService;

    @Mock
    private ComponentGraphService componentGraphService;

    @Mock
    private TemplateRenderer templateRenderer;

    @Mock
    private DocumentationGeneratorConfluenceProperties props;

    @InjectMocks
    private DocumentationGenerator documentationGenerator;

    @BeforeEach
    void setUp() {
        when(props.getRootPageId()).thenReturn(ROOT_PAGE_ID);
        when(props.getUrl()).thenReturn("https://confluence.example");
    }

    @Test
    void generate_withMocks_createsExpectedPages() {
        // Mocks for SystemComponent, Event and Command
        SystemComponent componentMock = mock(SystemComponent.class);
        when(componentMock.getName()).thenReturn("ComponentA");

        Event eventMock = mock(Event.class);
        when(eventMock.getMessageTypeName()).thenReturn("EventA");

        Command commandMock = mock(Command.class);
        when(commandMock.getMessageTypeName()).thenReturn("CommandA");

        // Mock for System
        System systemMock = mock(System.class);
        when(systemMock.getName()).thenReturn(SYSTEM_NAME);
        when(systemMock.getSystemComponents()).thenReturn(List.of(componentMock));
        when(systemMock.getEvents()).thenReturn(List.of(eventMock));
        when(systemMock.getCommands()).thenReturn(List.of(commandMock));

        ArchitectureModel model = ArchitectureModel.builder()
                .systems(List.of(systemMock))
                .build();

        // ConfluenceAdapter mocks
        when(confluenceAdapter.findOrCreatePageUnderAncestor(anyString(), anyString()))
                .thenAnswer(invocation -> UUID.randomUUID().toString());

        when(messageGraphService.getGraphs(any(), any())).thenReturn(List.of());

        // TemplateRenderer mocks
        when(templateRenderer.renderSystemPage(any(), eq(systemMock), any())).thenReturn("Rendered System Page");
        when(templateRenderer.renderIndexPage()).thenReturn("Rendered Index Page");
        when(templateRenderer.renderComponentPage(any(), eq(componentMock), any())).thenReturn("Rendered Component Page");
        when(templateRenderer.renderEventPage(eq(eventMock), any())).thenReturn("Rendered Event Page");
        when(templateRenderer.renderCommandPage(eq(commandMock), any())).thenReturn("Rendered Command Page");

        // Execution
        documentationGenerator.generate(model);

        // Verification
        InOrder pageOrder = inOrder(confluenceAdapter, systemGraphService);
        pageOrder.verify(confluenceAdapter, times(7)).findOrCreatePageUnderAncestor(anyString(), anyString());
        pageOrder.verify(systemGraphService).getGraph(eq(systemMock), any());
        verify(confluenceAdapter).findOrCreatePageUnderAncestor(eq(ROOT_PAGE_ID), eq(SYSTEM_NAME + " (System)"));
        verify(templateRenderer).renderSystemPage(any(), eq(systemMock), any());
        verify(templateRenderer).renderIndexPage();
        verify(templateRenderer).renderComponentPage(any(), eq(componentMock), any());
        verify(templateRenderer).renderEventPage(eq(eventMock), any());
        verify(templateRenderer).renderCommandPage(eq(commandMock), any());

        verify(systemGraphService).getGraph(eq(systemMock), any());
        verify(componentGraphService).getGraph(eq(componentMock), any());
        verify(messageGraphService).getGraphs(eq(eventMock), any());
        verify(messageGraphService).getGraphs(eq(commandMock), any());
        verify(confluenceAdapter, times(7)).updatePage(anyString(), anyString(), anyString(), anyString());
    }
}
