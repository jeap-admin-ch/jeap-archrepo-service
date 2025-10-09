package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.graph.ComponentGraphAttachmentService;
import ch.admin.bit.jeap.archrepo.docgen.graph.MessageGraphAttachmentService;
import ch.admin.bit.jeap.archrepo.docgen.graph.SystemGraphAttachmentService;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.ComponentGraphRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentationGenerator {

    private static final String SYSTEM_PAGE_NAME_POSTFIX = " (System)";

    private final ConfluenceAdapter confluenceAdapter;
    private final TemplateRenderer templateRenderer;
    private final DocumentationGeneratorConfluenceProperties props;
    private final MessageGraphAttachmentService messageGraphAttachmentService;
    private final SystemGraphAttachmentService systemGraphAttachmentService;
    private final ComponentGraphAttachmentService componentGraphAttachmentService;

    public void generate(ArchitectureModel model) {
        String rootPageId = confluenceAdapter.getPageByName(props.getRootPageName());
        GeneratorContext context = new GeneratorContext(model, rootPageId);
        model.getSystems().forEach(system -> generateSystem(context, rootPageId, system));
        log.info("Documentation generated, containing {} pages", context.getGeneratedPageIds().size());
        int deletedPageCount = confluenceAdapter.deleteOrphanPages(context.getRootPageId(), context.getGeneratedPageIds());
        log.info("Orphan cleanup done, deleted {} pages", deletedPageCount);
    }

    private void generateSystem(GeneratorContext context, String rootPageId, System system) {
        String graphAttachmentName = systemGraphAttachmentService.getSystemAttachmentNameIfExists(system.getName());
        String content = templateRenderer.renderSystemPage(context.getModel(), system, graphAttachmentName);
        String systemPageName = system.getName() + SYSTEM_PAGE_NAME_POSTFIX;
        String systemPageId = confluenceAdapter.addOrUpdatePageUnderAncestor(rootPageId, systemPageName, content);
        systemGraphAttachmentService.generateAttachment(system, systemPageId);

        String indexPageContent = templateRenderer.renderIndexPage();
        String componentsPageId = confluenceAdapter.addOrUpdatePageUnderAncestor(systemPageId, "Komponenten (" + system.getName() + ")", indexPageContent);
        system.getSystemComponents().forEach(systemComponent -> generateSystemComponent(context, componentsPageId, systemComponent));
        String eventsPageId = confluenceAdapter.addOrUpdatePageUnderAncestor(systemPageId, "Events (" + system.getName() + ")", indexPageContent);
        String commandsPageId = confluenceAdapter.addOrUpdatePageUnderAncestor(systemPageId, "Commands (" + system.getName() + ")", indexPageContent);
        system.getEvents().forEach(event -> generateEvent(context, eventsPageId, event));
        system.getCommands().forEach(command -> generateCommand(context, commandsPageId, command));
        context.addGeneratedPageIds(systemPageId, componentsPageId, eventsPageId, commandsPageId);
    }

    private void generateSystemComponent(GeneratorContext context, String ancestorId, SystemComponent systemComponent) {
        String graphAttachmentName = componentGraphAttachmentService.getComponentAttachmentNameIfExists(systemComponent.getName());
        String content = templateRenderer.renderComponentPage(context.getModel(), systemComponent, graphAttachmentName);
        String pageId = confluenceAdapter.addOrUpdatePageUnderAncestor(ancestorId, systemComponent.getName(), content);
        componentGraphAttachmentService.generateAttachment(systemComponent, pageId);
        context.addGeneratedPageIds(pageId);
    }

    private void generateEvent(GeneratorContext context, String ancestorId, Event event) {
        List<String> graphAttachmentNames = messageGraphAttachmentService.getAttachmentNames(event);

        String content = templateRenderer.renderEventPage(event, graphAttachmentNames);
        String pageId = confluenceAdapter.addOrUpdatePageUnderAncestor(ancestorId, event.getMessageTypeName(), content);
        messageGraphAttachmentService.generateAttachments(event, pageId);
        context.addGeneratedPageIds(pageId);
    }

    private void generateCommand(GeneratorContext context, String ancestorId, Command command) {
        List<String> graphAttachmentNames = messageGraphAttachmentService.getAttachmentNames(command);

        String content = templateRenderer.renderCommandPage(command, graphAttachmentNames);
        String pageId = confluenceAdapter.addOrUpdatePageUnderAncestor(ancestorId, command.getMessageTypeName(), content);
        messageGraphAttachmentService.generateAttachments(command, pageId);
        context.addGeneratedPageIds(pageId);
    }
}
