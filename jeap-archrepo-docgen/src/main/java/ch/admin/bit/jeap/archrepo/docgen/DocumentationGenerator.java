package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.GeneratorContext.PageRef;
import ch.admin.bit.jeap.archrepo.docgen.graph.ComponentGraphService;
import ch.admin.bit.jeap.archrepo.docgen.graph.MessageGraphService;
import ch.admin.bit.jeap.archrepo.docgen.graph.RenderedReactionGraph;
import ch.admin.bit.jeap.archrepo.docgen.graph.SystemGraphService;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static ch.admin.bit.jeap.archrepo.docgen.GeneratorContext.key;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentationGenerator {

    private static final String SYSTEM_PAGE_NAME_POSTFIX = " (System)";

    private final ConfluenceAdapter confluenceAdapter;
    private final TemplateRenderer templateRenderer;
    private final DocumentationGeneratorConfluenceProperties props;
    private final MessageGraphService messageGraphService;
    private final SystemGraphService systemGraphService;
    private final ComponentGraphService componentGraphService;

    public void generate(ArchitectureModel model) {
        String rootPageId = props.getRootPageId();
        GeneratorContext context = new GeneratorContext(model, rootPageId, props.getUrl());

        model.getSystems().forEach(system -> ensureSystemPages(context, system));
        model.getSystems().forEach(system -> renderSystemPages(context, system));

        log.info("Documentation generated, containing {} pages", context.getGeneratedPageIds().size());
        int deletedPageCount = confluenceAdapter.deleteOrphanPages(context.getRootPageId(), context.getGeneratedPageIds());
        log.info("Orphan cleanup done, deleted {} pages", deletedPageCount);
    }

    private void ensureSystemPages(GeneratorContext context, System system) {
        String systemKey = key(system.getName());
        PageRef systemPage = ensurePage(context, context.getRootPageId(), system.getName() + SYSTEM_PAGE_NAME_POSTFIX);
        context.getSystemPages().put(systemKey, systemPage);

        PageRef componentsPage = ensurePage(context, systemPage.id(), "Komponenten (" + system.getName() + ")");
        PageRef eventsPage = ensurePage(context, systemPage.id(), "Events (" + system.getName() + ")");
        PageRef commandsPage = ensurePage(context, systemPage.id(), "Commands (" + system.getName() + ")");
        context.getComponentIndexPages().put(systemKey, componentsPage);
        context.getEventIndexPages().put(systemKey, eventsPage);
        context.getCommandIndexPages().put(systemKey, commandsPage);

        system.getSystemComponents().forEach(component -> {
            PageRef page = ensurePage(context, componentsPage.id(), component.getName());
            context.getComponentPages().putIfAbsent(key(component.getName()), page);
            context.getComponentEntityPages().put(component, page);
        });
        system.getEvents().forEach(event -> {
            PageRef page = ensurePage(context, eventsPage.id(), event.getMessageTypeName());
            context.addMessagePage(event.getMessageTypeName(), page);
            context.getMessageEntityPages().put(event, page);
        });
        system.getCommands().forEach(command -> {
            PageRef page = ensurePage(context, commandsPage.id(), command.getMessageTypeName());
            context.addMessagePage(command.getMessageTypeName(), page);
            context.getMessageEntityPages().put(command, page);
        });
    }

    private PageRef ensurePage(GeneratorContext context, String ancestorId, String title) {
        String id = confluenceAdapter.findOrCreatePageUnderAncestor(ancestorId, title);
        context.addGeneratedPageIds(id);
        return new PageRef(id, ancestorId, title);
    }

    private void renderSystemPages(GeneratorContext context, System system) {
        String systemKey = key(system.getName());
        PageRef systemPage = context.getSystemPages().get(systemKey);
        RenderedReactionGraph graph = systemGraphService.getGraph(system, context::resolveNodeUrl);
        updatePage(systemPage, templateRenderer.renderSystemPage(context.getModel(), system, graph));

        String indexContent = templateRenderer.renderIndexPage();
        updatePage(context.getComponentIndexPages().get(systemKey), indexContent);
        updatePage(context.getEventIndexPages().get(systemKey), indexContent);
        updatePage(context.getCommandIndexPages().get(systemKey), indexContent);

        system.getSystemComponents().forEach(component -> renderComponent(context, component));
        system.getEvents().forEach(event -> renderEvent(context, event));
        system.getCommands().forEach(command -> renderCommand(context, command));
    }

    private void renderComponent(GeneratorContext context, SystemComponent component) {
        RenderedReactionGraph graph = componentGraphService.getGraph(component, context::resolveNodeUrl);
        String content = templateRenderer.renderComponentPage(context.getModel(), component, graph);
        updatePage(context.getComponentEntityPages().get(component), content);
    }

    private void renderEvent(GeneratorContext context, Event event) {
        List<RenderedReactionGraph> graphs = messageGraphService.getGraphs(event, context::resolveNodeUrl);
        updatePage(context.getMessageEntityPages().get(event), templateRenderer.renderEventPage(event, graphs));
    }

    private void renderCommand(GeneratorContext context, Command command) {
        List<RenderedReactionGraph> graphs = messageGraphService.getGraphs(command, context::resolveNodeUrl);
        updatePage(context.getMessageEntityPages().get(command), templateRenderer.renderCommandPage(command, graphs));
    }

    private void updatePage(PageRef page, String content) {
        confluenceAdapter.updatePage(page.id(), page.ancestorId(), page.title(), content);
    }
}
