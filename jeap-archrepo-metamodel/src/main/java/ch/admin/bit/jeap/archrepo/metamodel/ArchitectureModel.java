package ch.admin.bit.jeap.archrepo.metamodel;

import ch.admin.bit.jeap.archrepo.metamodel.domainevents.CommandRemoved;
import ch.admin.bit.jeap.archrepo.metamodel.domainevents.EventRemoved;
import ch.admin.bit.jeap.archrepo.metamodel.domainevents.SystemComponentRemoved;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationType;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentType;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableList;

@EqualsAndHashCode
@ToString
public class ArchitectureModel {
    private final List<Team> teams;
    private final List<System> systems;
    private final String openApiBaseUrl;

    private ArchitectureModel(List<Team> teams, List<System> systems, String openApiBaseUrl) {
        this.teams = teams;
        this.systems = systems;
        this.openApiBaseUrl = openApiBaseUrl;
    }

    public List<System> getSystems() {
        return unmodifiableList(systems);
    }

    private static boolean systemNameOrAliasMatches(String systemName, System system) {
        return systemName.equalsIgnoreCase(system.getName()) ||
                (system.getAliases() != null && system.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(systemName)));
    }

    public Optional<System> findSystem(String systemName) {
        return systems.stream()
                .filter(system -> systemNameOrAliasMatches(systemName, system))
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    public <T extends Relation> List<T> getAllRelationsByType(Class<T> relationClass) {
        return systems.stream()
                .flatMap(system -> system.getRelations().stream().filter(relation -> relationClass.isAssignableFrom(relation.getClass())))
                .map(rel -> (T) rel)
                .toList();
    }

    public List<RestApi> getAllRestApis() {
        return systems.stream()
                .flatMap(system -> system.getRestApis().stream())
                .toList();
    }

    /**
     * Housekeeping of the architecture model, currently only for REST API elements. Removes
     * <ol>
     *     <li>automatically imported RestApiRelation which have no "last seen date" set or which are older than 3 Months</li>
     *     <li>Unused RestOperations</li>
     *     <li>Unused RestResource</li>
     * </ol>
     */
    public void cleanup() {
        ArchitectureModelHelper.cleanup(this);
    }

    public List<Relation> getAllActiveRelationsByType(RelationType relationType) {
        return systems.stream()
                .flatMap(system -> system.getActiveRelations().stream().filter(relation -> relation.getType() == relationType))
                .toList();
    }

    public List<Relation> getAllRelations() {
        return systems.stream()
                .flatMap(system -> system.getRelations().stream())
                .toList();
    }

    public List<RestApiRelation> getRestApiRelationsWithoutPact() {
        return systems.stream()
                .flatMap(system -> system.getActiveRelations().stream())
                .filter(RestApiRelation.class::isInstance)
                .map(RestApiRelation.class::cast)
                .filter(relation -> relation.getPactUrl() == null)
                .toList();
    }

    public Optional<SystemComponent> findSystemComponent(String componentName) {
        return systems.stream()
                .flatMap(system -> system.findSystemComponent(componentName).stream())
                .findFirst();
    }

    public void removeAllByImporter(Importer importer) {
        Set<SystemComponentRemoved> systemComponentRemovedList = new HashSet<>();
        Set<EventRemoved> eventRemovedList = new HashSet<>();
        Set<CommandRemoved> commandRemovedList = new HashSet<>();

        getAllSystems()
                .forEach(s -> {
                    RemovedElements removedElements = s.removeAllByImporter(importer);
                    systemComponentRemovedList.addAll(removedElements.systemComponentRemovedList());
                    eventRemovedList.addAll(removedElements.eventRemovedList());
                    commandRemovedList.addAll(removedElements.commandRemovedList());
                });

        getAllSystems()
                .forEach(s -> {
                    systemComponentRemovedList.forEach(s::onSystemComponentRemoved);
                    eventRemovedList.forEach(s::onEventRemoved);
                    commandRemovedList.forEach(s::onCommandRemoved);
                });
    }

    public List<SystemComponent> getAllSystemComponentsByImporter(Importer importer) {
        return getAllSystems()
                .flatMap(s -> s.getSystemComponents().stream())
                .filter(s -> s.getImporter() == importer)
                .toList();
    }

    public List<MessageType> getAllMessageTypes() {
        return getAllSystems()
                .flatMap(s -> Stream.concat(s.getCommands().stream(), s.getEvents().stream()))
                .toList();
    }

    public Optional<String> getRestApiForSystemComponent(SystemComponent systemComponent) {
        if (SystemComponentType.BACKEND_SERVICE.equals(systemComponent.getType()) || SystemComponentType.SELF_CONTAINED_SYSTEM.equals(systemComponent.getType())) {

            Optional<OpenApiSpec> openApiSpec = systemComponent.getParent().getOpenApiSpecs().stream()
                    .filter(o -> o.getProvider().equals(systemComponent)).findFirst();

            if (openApiSpec.isPresent()) {
                return Optional.of(createOpenApiSpecSwaggerUrl(systemComponent));
            }
        }
        return Optional.empty();
    }

    public List<String> getSystemComponentsWithoutOpenApiSpec() {
        return this.getAllSystemComponents()
                .filter(sc -> SystemComponentType.BACKEND_SERVICE.equals(sc.getType()) || SystemComponentType.SELF_CONTAINED_SYSTEM.equals(sc.getType()))
                .filter(sc -> sc.getParent().getOpenApiSpecs().stream().filter(o -> o.getProvider().equals(sc)).findFirst().isEmpty())
                .map(SystemComponent::getName)
                .toList();
    }

    private String createOpenApiSpecSwaggerUrl(SystemComponent systemComponent) {
        return (openApiBaseUrl + systemComponent.getParent().getName() + "/" + systemComponent.getName()).toLowerCase();
    }

    public void remove(SystemComponent component) {
        getAllSystems()
                .forEach(system -> system.removeSystemComponent(component));

        SystemComponentRemoved systemComponentRemoved = SystemComponentRemoved.of(component);
        getAllSystems()
                .forEach(system -> system.onSystemComponentRemoved(systemComponentRemoved));
    }

    public Optional<MessageType> findMessageType(String messageTypeName) {
        return getAllSystems()
                .map(system -> system.findMessageType(messageTypeName))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private Stream<System> getAllSystems() {
        return systems.stream();
    }

    private Stream<SystemComponent> getAllSystemComponents() {
        return getAllSystems().flatMap(system -> system.getSystemComponents().stream());
    }

    public Map<String, String> getAllSystemComponentNamesWithSystemName() {
        return getAllSystemComponents()
                .collect(Collectors.toMap(SystemComponent::getName, component -> component.getParent().getName()));
    }

    public static ArchitectureModelBuilder builder() {
        return new ArchitectureModelBuilder();
    }

    public static class ArchitectureModelBuilder {
        private List<Team> teams = new ArrayList<>();
        private List<System> systems = new ArrayList<>();

        private String openApiBaseUrl;

        ArchitectureModelBuilder() {
        }

        public ArchitectureModelBuilder teams(List<Team> teams) {
            this.teams = teams;
            return this;
        }

        public ArchitectureModelBuilder systems(List<System> systems) {
            this.systems = systems;
            return this;
        }

        public ArchitectureModelBuilder openApiBaseUrl(String openApiBaseUrl) {
            this.openApiBaseUrl = openApiBaseUrl;
            return this;
        }


        public ArchitectureModel build() {
            return new ArchitectureModel(teams, systems, openApiBaseUrl);
        }
    }
}
