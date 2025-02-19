package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentationGenerator {

    private static final String SYSTEM_PAGE_NAME_POSTFIX = " (System)";

    private final ConfluenceAdapter confluenceAdapter;
    private final TemplateRenderer templateRenderer;
    private final DocumentationGeneratorConfluenceProperties props;

    public void generate(ArchitectureModel model) {
        String rootPageId = confluenceAdapter.getPageByName(props.getRootPageName());
        GeneratorContext context = new GeneratorContext(model, rootPageId);
        model.getSystems().forEach(system -> generateSystem(context, rootPageId, system));
        log.info("Documentation generated, containing {} pages", context.getGeneratedPageIds().size());

        int deletedPageCount = confluenceAdapter.deleteOrphanPages(context.getRootPageId(), context.getGeneratedPageIds());
        log.info("Orphan cleanup done, deleted {} pages", deletedPageCount);
    }

    private void generateSystem(GeneratorContext context, String rootPageId, System system) {
        String content = templateRenderer.renderSystemPage(context.getModel(), system);
        String systemPageName = system.getName() + SYSTEM_PAGE_NAME_POSTFIX;
        String systemPageId = confluenceAdapter.addOrUpdatePageUnderAncestor(rootPageId, systemPageName, content);
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
        String content = templateRenderer.renderComponentPage(context.getModel(), systemComponent);
        String pageId = confluenceAdapter.addOrUpdatePageUnderAncestor(ancestorId, systemComponent.getName(), content);
        context.addGeneratedPageIds(pageId);
    }

    private void generateEvent(GeneratorContext context, String ancestorId, Event event) {
        String content = templateRenderer.renderEventPage(event);
        String pageId = confluenceAdapter.addOrUpdatePageUnderAncestor(ancestorId, event.getMessageTypeName(), content);
        context.addGeneratedPageIds(pageId);
    }

    private void generateCommand(GeneratorContext context, String ancestorId, Command command) {
        String content = templateRenderer.renderCommandPage(command);
        String pageId = confluenceAdapter.addOrUpdatePageUnderAncestor(ancestorId, command.getMessageTypeName(), content);
        context.addGeneratedPageIds(pageId);
    }
}
