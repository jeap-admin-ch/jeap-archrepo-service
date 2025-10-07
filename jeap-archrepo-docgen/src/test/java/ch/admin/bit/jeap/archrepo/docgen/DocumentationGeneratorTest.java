package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.graph.MessageGraphAttachmentService;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentationGeneratorTest {

    private static final String ROOT_PAGE_NAME = "rootPageName";
    private static final String ROOT_PAGE_ID = "rootPageId";
    private static final String SYSTEM_NAME = "MockSystem";

    @Mock
    private ConfluenceAdapter confluenceAdapter;

    @Mock
    private MessageGraphAttachmentService messageGraphAttachmentService;

    @Mock
    private TemplateRenderer templateRenderer;

    @Mock
    private DocumentationGeneratorConfluenceProperties props;

    @InjectMocks
    private DocumentationGenerator documentationGenerator;

    @BeforeEach
    void setUp() {
        when(props.getRootPageName()).thenReturn(ROOT_PAGE_NAME);
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

        // Handling of the ConfluenceAdapter-Mocks
        when(confluenceAdapter.getPageByName(ROOT_PAGE_NAME)).thenReturn(ROOT_PAGE_ID);
        when(confluenceAdapter.addOrUpdatePageUnderAncestor(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> UUID.randomUUID().toString());

        // Handling of the Attachment-Mocks
        when(messageGraphAttachmentService.getAttachmentNames(any())).thenReturn(List.of("graph1.png"));
        doNothing().when(messageGraphAttachmentService).generateAttachments(any(), anyString());

        // Handling of the TemplateRenderer-Mocks
        when(templateRenderer.renderSystemPage(any(), any())).thenReturn("Rendered System Page");
        when(templateRenderer.renderIndexPage()).thenReturn("Rendered Index Page");
        when(templateRenderer.renderComponentPage(any(), any())).thenReturn("Rendered Component Page");
        when(templateRenderer.renderEventPage(any(), any())).thenReturn("Rendered Event Page");
        when(templateRenderer.renderCommandPage(any(), any())).thenReturn("Rendered Command Page");

        // Ausführung
        documentationGenerator.generate(model);

        // Verifikation
        verify(confluenceAdapter).addOrUpdatePageUnderAncestor(eq(ROOT_PAGE_ID), eq(SYSTEM_NAME + " (System)"), anyString());
        verify(templateRenderer).renderSystemPage(any(), eq(systemMock));
        verify(templateRenderer).renderIndexPage();
        verify(templateRenderer).renderComponentPage(any(), eq(componentMock));
        verify(templateRenderer).renderEventPage(eq(eventMock), any());
        verify(templateRenderer).renderCommandPage(eq(commandMock), any());
    }
}