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
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableList;

@EqualsAndHashCode
@ToString
@Slf4j
public class ArchitectureModel {
    private final List<Team> teams;
    private final List<System> systems;
    private final List<System> removedSystems = new ArrayList<>();
    private final List<Team> removedTeams = new ArrayList<>();
    private final String openApiBaseUrl;
    @EqualsAndHashCode.Exclude
    private boolean systemsAndTeamsSealed;

    private ArchitectureModel(List<Team> teams, List<System> systems, String openApiBaseUrl) {
        this.teams = new ArrayList<>(teams);
        this.systems = new ArrayList<>(systems);
        this.openApiBaseUrl = openApiBaseUrl;
    }

    public List<System> getSystems() {
        return unmodifiableList(systems);
    }

    public List<Team> getTeams() {
        return unmodifiableList(teams);
    }

    public List<System> getRemovedSystems() {
        return unmodifiableList(removedSystems);
    }

    public List<Team> getRemovedTeams() {
        return unmodifiableList(removedTeams);
    }

    /**
     * The configuration ({@code archrepo.model}) is the master of systems and teams. Once onboarding has applied
     * it, the model is sealed: adding or removing a system or a team - by an importer, say - then throws instead
     * of quietly creating data outside the declaration. Everything below systems (components, messages, relations,
     * REST APIs, ...) stays open to the importers.
     */
    public void sealSystemsAndTeams() {
        systemsAndTeamsSealed = true;
    }

    private void requireUnsealed(String change, String name) {
        if (systemsAndTeamsSealed) {
            throw new IllegalStateException(("Cannot %s '%s': systems and teams are declared in the archrepo.model "
                    + "configuration and only onboarding may change them").formatted(change, name));
        }
    }

    public void addTeam(Team team) {
        requireUnsealed("add team", team.getName());
        teams.add(team);
    }

    public void addSystem(System system) {
        requireUnsealed("add system", system.getName());
        systems.add(system);
    }

    public Optional<Team> findTeamByNameIgnoreCase(String name) {
        return teams.stream().filter(team -> team.getName().equalsIgnoreCase(name)).findFirst();
    }

    public Optional<System> findSystemByNameIgnoreCase(String name) {
        return systems.stream().filter(system -> system.getName().equalsIgnoreCase(name)).findFirst();
    }

    public void removeSystem(System removed) {
        requireUnsealed("remove system", removed.getName());
        for (System remaining : systems) {
            if (remaining == removed) {
                continue;
            }
            // A REST relation may be owned by a different system than its API.
            remaining.getRelations().stream()
                    .filter(RestApiRelation.class::isInstance)
                    .map(RestApiRelation.class::cast)
                    .filter(relation -> removed.getRestApis().contains(relation.getRestApi()))
                    .toList().forEach(remaining::removeRestApiRelation);
            removed.getSystemComponentsIncludingUndeployed()
                    .forEach(component -> remaining.onSystemComponentRemoved(SystemComponentRemoved.of(component)));
            removed.getEvents().forEach(event -> remaining.onEventRemoved(EventRemoved.of(event)));
            removed.getCommands().forEach(command -> remaining.onCommandRemoved(CommandRemoved.of(command)));
        }
        if (systems.remove(removed)) {
            removedSystems.add(removed);
        }
    }

    public void removeTeam(Team team) {
        requireUnsealed("remove team", team.getName());
        if (teams.remove(team)) {
            removedTeams.add(team);
        }
    }

    private static boolean nameMatches(String systemName, System system) {
        return systemName.equalsIgnoreCase(system.getName());
    }

    private static boolean aliasMatches(String systemName, System system) {
        return system.getAliases() != null
                && system.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(systemName));
    }

    /**
     * The system of that name, or - only where no system carries the name - the one holding it as an alias.
     * <p>
     * Names and aliases share one namespace, so a system may carry another system's name as an alias. Matching
     * both in one pass would leave the order of {@link #systems} to decide which of the two is returned, and
     * that order is whatever the database happened to hand back: an importer would file a message or a
     * component under the wrong system, and every reader of the model would follow it there.
     */
    public Optional<System> findSystem(String systemName) {
        return systems.stream()
                .filter(system -> nameMatches(systemName, system))
                .findFirst()
                .or(() -> systems.stream()
                        .filter(system -> aliasMatches(systemName, system))
                        .findFirst());
    }

    public <T extends Relation> List<T> getAllRelationsByType(Class<T> relationClass) {
        return systems.stream()
                .flatMap(system -> system.getRelations().stream().filter(relation -> relationClass.isAssignableFrom(relation.getClass())))
                .map(relationClass::cast)
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
     * REST APIs imported from an OpenAPI spec count as used, so they are kept even if no relation references them.
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

    /**
     * Not {@link #getAllActiveRelationsByType}: a relation to or from an undeployed component is excluded too,
     * so a missing Pact test for one left is not reported as something to fix.
     */
    public List<RestApiRelation> getRestApiRelationsWithoutPact() {
        return systems.stream()
                .flatMap(system -> system.getActiveRelations().stream())
                .filter(RestApiRelation.class::isInstance)
                .map(RestApiRelation.class::cast)
                .filter(relation -> relation.getPactUrl() == null)
                .filter(relation -> !involvesUndeployedComponent(relation))
                .toList();
    }

    public Optional<SystemComponent> findSystemComponent(String componentName) {
        return systems.stream()
                .flatMap(system -> system.findSystemComponent(componentName).stream())
                .findFirst();
    }

    /**
     * The component already known as {@code componentName}, moved onto {@code system} first - with its REST APIs,
     * OpenAPI spec and database schema, see {@link System#moveSystemComponentTo} - if it currently lives under a
     * different one. An importer resolves a component's system by a name or alias that can be
     * reassigned from one surviving system to another between runs; without this, the importer would not find
     * the component under the newly resolved system and recreate it there, colliding with the database's global
     * unique index on the component's name while the old row is still present under its previous owner. Empty if
     * the model has no component of that name anywhere - the caller then creates one.
     */
    public Optional<SystemComponent> findOrRelocateSystemComponent(System system, String componentName) {
        Optional<SystemComponent> local = system.findSystemComponent(componentName);
        if (local.isPresent()) {
            return local;
        }
        return findSystemComponent(componentName)
                .map(component -> relocateIfItsNameBelongsTo(component, system))
                .or(() -> reviveRemovedSystemComponent(componentName, system));
    }

    /**
     * A component moves only to the system its name belongs to - its prefix, resolved as a name or alias. That is
     * what changes when an alias moves from one system to another. An importer that resolves the system some
     * other way (RHOS: from the namespace) can report a component under a system its name does not belong to, or
     * under two systems; moving it each time would move it back and forth on every run. It stays where it is
     * instead, and the conflicting report is logged as an error so it gets fixed at the source.
     */
    private SystemComponent relocateIfItsNameBelongsTo(SystemComponent component, System reporting) {
        if (nameBelongsTo(component.getName(), reporting)) {
            relocate(component, reporting);
        } else {
            logMisplacedReport(component.getName(), reporting, component.getParent());
        }
        return component;
    }

    private boolean nameBelongsTo(String componentName, System system) {
        return systemOfName(componentName).filter(owner -> owner == system).isPresent();
    }

    private Optional<System> systemOfName(String componentName) {
        return findSystem(componentName.split("-")[0]);
    }

    private void logMisplacedReport(String componentName, System reporting, System keeping) {
        log.error("System component {} is reported for system {}, but its name belongs to {}; it stays in system {}",
                componentName, reporting.getName(),
                systemOfName(componentName).map(owner -> "system " + owner.getName()).orElse("no system"),
                keeping.getName());
    }

    /**
     * A component removed earlier in this update - by an importer that no longer reports it, while a later one
     * still does - comes back instead of being created again, see {@link System#reviveRemovedSystemComponent}.
     */
    private Optional<SystemComponent> reviveRemovedSystemComponent(String componentName, System reporting) {
        return getAllSystemsIncludingRemoved()
                .filter(former -> former.getRemovedSystemComponents().stream()
                        .anyMatch(component -> component.getName().equalsIgnoreCase(componentName)))
                .findFirst()
                .flatMap(former -> former.reviveRemovedSystemComponent(componentName,
                        revivalTarget(componentName, reporting, former)));
    }

    /** Where it was, unless its name belongs to the reporting system - the same rule as for a move. */
    private System revivalTarget(String componentName, System reporting, System former) {
        if (former == reporting || nameBelongsTo(componentName, reporting) || !systems.contains(former)) {
            return reporting;
        }
        logMisplacedReport(componentName, reporting, former);
        return former;
    }

    /**
     * Components removed from any system - retained or removed - in this update, for the repository to delete.
     * They are not deleted by orphan removal, see the comment on {@code System.systemComponents}.
     */
    public List<SystemComponent> getRemovedSystemComponents() {
        return getAllSystemsIncludingRemoved().flatMap(system -> system.getRemovedSystemComponents().stream()).toList();
    }

    /** The REST APIs, OpenAPI specs and database schemas removed from any system in this update, to be deleted. */
    public List<MutableDomainEntity> getRemovedArtifacts() {
        return getAllSystemsIncludingRemoved().flatMap(system -> system.getRemovedArtifacts().stream()).toList();
    }

    private Stream<System> getAllSystemsIncludingRemoved() {
        return Stream.concat(systems.stream(), removedSystems.stream());
    }

    private static SystemComponent relocate(SystemComponent component, System target) {
        component.getParent().moveSystemComponentTo(component, target);
        return component;
    }

    /**
     * Whether the name is a component this model has and it is undeployed - what a diagram or a relation list
     * checks before drawing it, so that a relation to or from a component that left no longer reads as part of
     * the current architecture. A name the model does not have - another system's business name in an event
     * relation, for instance - answers false: this is not the check that catches that.
     */
    public boolean isUndeployedComponent(String componentName) {
        return componentName != null && findSystemComponent(componentName).map(SystemComponent::isUndeployed).orElse(false);
    }

    /**
     * Whether either end of the relation is an undeployed component - a relation to or from one left no longer
     * reads as part of the current architecture, the rule every API and doc surface applies before showing a
     * relation.
     */
    public boolean involvesUndeployedComponent(Relation relation) {
        return isUndeployedComponent(relation.getConsumerName()) || isUndeployedComponent(relation.getProviderName());
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
                .flatMap(s -> s.getSystemComponentsIncludingUndeployed().stream())
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
                .filter(sc -> !sc.isUndeployed())
                .filter(sc -> SystemComponentType.BACKEND_SERVICE.equals(sc.getType()) || SystemComponentType.SELF_CONTAINED_SYSTEM.equals(sc.getType()))
                .filter(sc -> sc.getParent().getOpenApiSpecs().stream().filter(o -> o.getProvider().equals(sc)).findFirst().isEmpty())
                .map(SystemComponent::getName)
                .toList();
    }

    /**
     * The Swagger UI deep link of a component - an absolute URL, because a browser follows it. Built from
     * {@code archrepo.openapi-base-url}, which every instance configures with the browser-reachable prefix.
     */
    public String createOpenApiSpecSwaggerUrl(SystemComponent systemComponent) {
        return (openApiBaseUrl + systemComponent.getParent().getName() + "/" + systemComponent.getName())
                .toLowerCase(Locale.ROOT);
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
        return getAllSystems().flatMap(system -> system.getSystemComponentsIncludingUndeployed().stream());
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
