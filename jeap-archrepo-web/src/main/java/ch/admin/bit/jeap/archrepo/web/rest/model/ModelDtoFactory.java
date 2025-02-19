package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Relation;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.relation.CommandRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Component
class ModelDtoFactory {

    ModelDto createModelDto(ArchitectureModel model) {
        return ModelDto.builder()
                .systems(map(model.getSystems(), this::createSystemDto))
                .build();
    }

    private SystemDto createSystemDto(System system) {
        return SystemDto.builder()
                .aliases(Set.copyOf(system.getAliases()))
                .name(system.getName())
                .ownedBy(system.getDefaultOwner() != null ? system.getDefaultOwner().getName() : null)
                .description(system.getDescription())
                .systemComponents(map(system.getSystemComponents(), this::createSystemComponentDto))
                .build();
    }

    private SystemComponentDto createSystemComponentDto(SystemComponent component) {
        return SystemComponentDto.builder()
                .type(component.getType())
                .name(component.getName())
                .importer(component.getImporter())
                .ownedBy(component.getOwnedBy() == null ? null : component.getOwnedBy().getName())
                .description(component.getDescription())
                .build();
    }

    private static <E, R> List<R> map(List<E> elements, Function<E, R> mapper) {
        return elements.stream()
                .map(mapper)
                .toList();
    }

    public List<RelationDto> createRelationDtos(ArchitectureModel model, System system) {
        final Map<String, String> servicesAndSystems = model.getAllSystemComponentNamesWithSystemName();
        return model.getSystems().stream()
                .flatMap(s -> s.getActiveRelations().stream())
                .filter(r -> consumerOrProducerIsForSystem(r, system.getName(), servicesAndSystems))
                .map(r -> createRelationDto(servicesAndSystems, r))
                .toList();
    }

    private boolean consumerOrProducerIsForSystem(Relation relation, String system, Map<String, String> servicesAndSystems) {
        return system.equalsIgnoreCase(servicesAndSystems.get(relation.getConsumerName())) ||
               system.equalsIgnoreCase(servicesAndSystems.get(relation.getProviderName()));
    }

    RelationDto createRelationDto(Map<String, String> componentNameToSystemName, Relation relation) {
        RelationDto.RelationDtoBuilder builder = RelationDto.builder()
                .relationType(relation.getType())
                .consumerSystem(componentNameToSystemName.get(relation.getConsumerName()))
                .consumer(relation.getConsumerName())
                .providerSystem(componentNameToSystemName.get(relation.getProviderName()))
                .provider(relation.getProviderName());

        switch (relation) {
            case RestApiRelation restApiRelation -> builder
                    .method(restApiRelation.getRestApi().getMethod())
                    .path(restApiRelation.getRestApi().getPath());
            case EventRelation eventRelation -> builder
                    .messageType(eventRelation.getEventName());
            case CommandRelation commandRelation -> builder
                    .messageType(commandRelation.getCommandName());
            default -> {
            }
        }

        return builder.build();
    }

    RestApiRelationDto createRestApiRelationDto(Map<String, String> componentNameToSystemName, RestApiRelation relation) {
        return RestApiRelationDto.builder()
                .consumerSystem(componentNameToSystemName.get(relation.getConsumerName()))
                .consumer(relation.getConsumerName())
                .providerSystem(componentNameToSystemName.get(relation.getProviderName()))
                .provider(relation.getProviderName())
                .method(relation.getRestApi().getMethod())
                .path(relation.getRestApi().getPath())
                .build();
    }
}
