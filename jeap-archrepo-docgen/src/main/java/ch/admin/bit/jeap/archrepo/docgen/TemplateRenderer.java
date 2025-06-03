package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.plantuml.PlantUmlRenderer;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.relation.CommandRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;

@Component
@RequiredArgsConstructor
class TemplateRenderer {

    private final ITemplateEngine templateEngine;
    private final PlantUmlRenderer plantUmlRenderer;

    String renderIndexPage() {
        Context context = new Context(Locale.GERMAN);
        return templateEngine.process("index", context).trim();
    }

    String renderSystemPage(ArchitectureModel model, System system) {
        List<SystemEvent> systemEvents = system.getEvents().stream()
                .map(e -> createSystemEvent(model, e))
                .sorted(comparing(SystemEvent::getName))
                .toList();
        List<SystemCommand> systemCommands = system.getCommands().stream()
                .map(e -> createSystemCommand(model, e))
                .sorted(comparing(SystemCommand::getName))
                .toList();
        SystemContext systemContext = SystemContext.of(model, system);

        system.sortSystemComponents();

        Context context = new Context(Locale.GERMAN);
        context.setVariable("system", system);
        context.setVariable("systemEvents", systemEvents);
        context.setVariable("systemCommands", systemCommands);
        String plantUmLSource = plantUmlRenderer.renderSystemContextView(systemContext);
        context.setVariable("contextViewPlantUml", plantUmLSource);
        context.setVariable("plantUmlMacroId", stableMacroUuid(system.getName()));
        return templateEngine.process("system", context).trim();
    }

    private SystemEvent createSystemEvent(ArchitectureModel model, Event e) {
        Set<String> providerNames = getRelationsForEvent(model, e)
                .map(EventRelation::getProviderName)
                .filter(Objects::nonNull)
                .collect(toSet());
        Set<String> consumerNames = getRelationsForEvent(model, e)
                .map(EventRelation::getConsumerName)
                .filter(Objects::nonNull)
                .collect(toSet());
        return new SystemEvent(e.getMessageTypeName(), providerNames, consumerNames);
    }

    private SystemCommand createSystemCommand(ArchitectureModel model, Command e) {
        Set<String> senderNames = getRelationsForCommand(model, e)
                .map(CommandRelation::getProviderName)
                .filter(Objects::nonNull)
                .collect(toSet());
        Set<String> receiverNames = getRelationsForCommand(model, e)
                .map(CommandRelation::getConsumerName)
                .filter(Objects::nonNull)
                .collect(toSet());
        return new SystemCommand(e.getMessageTypeName(), senderNames, receiverNames);
    }

    private Stream<EventRelation> getRelationsForEvent(ArchitectureModel model, Event event) {
        return model.getAllRelationsByType(EventRelation.class).stream()
                .filter(r -> r.getEventName().equals(event.getMessageTypeName()));
    }

    private Stream<CommandRelation> getRelationsForCommand(ArchitectureModel model, Command command) {
        return model.getAllRelationsByType(CommandRelation.class).stream()
                .filter(r -> r.getCommandName().equals(command.getMessageTypeName()));
    }

    String renderComponentPage(ArchitectureModel model, SystemComponent systemComponent) {
        ComponentContext componentContext = ComponentContext.of(model, systemComponent);

        Context context = new Context(Locale.GERMAN);
        context.setVariable("systemComponent", systemComponent);
        String plantUmLSource = plantUmlRenderer.renderComponentContextView(componentContext);
        context.setVariable("contextViewPlantUml", plantUmLSource);
        context.setVariable("plantUmlMacroId", stableMacroUuid(systemComponent.getName()));

        context.setVariable("consumedRestApiRelations", componentContext.getConsumedRestApiRelations());
        context.setVariable("producedEventRelations", componentContext.getProducedEventsGroupedByEvent());
        context.setVariable("sentCommandRelations", componentContext.getSentCommandsGroupedByCommand());

        context.setVariable("providedRestApiRelations", componentContext.getProvidedRestApiRelationsGroupedByPath());
        context.setVariable("consumedEventRelations", componentContext.getConsumedEventsGroupedByEvent());
        context.setVariable("receivedCommandRelations", componentContext.getReceivedCommandsGroupedByCommand());

        context.setVariable("openApiSpecUrl", componentContext.getOpenApiSpecUrl());
        context.setVariable("reactions", componentContext.getReactionStatisticsViews());
        return templateEngine.process("system-component", context).trim();
    }

    private String stableMacroUuid(String plantUmLSource) {
        return UUID.nameUUIDFromBytes(plantUmLSource.getBytes(StandardCharsets.UTF_8)).toString();
    }

    String renderEventPage(Event event) {
        Context context = new Context(Locale.GERMAN);
        context.setVariable("messageType", event);
        return templateEngine.process("event", context).trim();
    }

    String renderCommandPage(Command command) {
        Context context = new Context(Locale.GERMAN);
        context.setVariable("messageType", command);
        return templateEngine.process("command", context).trim();
    }
}
