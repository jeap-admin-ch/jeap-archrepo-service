package ch.admin.bit.jeap.archrepo.metamodel;

import ch.admin.bit.jeap.archrepo.metamodel.database.SystemComponentDatabaseSchema;
import ch.admin.bit.jeap.archrepo.metamodel.domainevents.CommandRemoved;
import ch.admin.bit.jeap.archrepo.metamodel.domainevents.EventRemoved;
import ch.admin.bit.jeap.archrepo.metamodel.domainevents.SystemComponentRemoved;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.metamodel.relation.*;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Persistable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.admin.bit.jeap.archrepo.metamodel.Importable.filterByImporter;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;

@SuppressWarnings({"findbugs:RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "java:S1144"})
// Sonar does not always play well with lombok
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
/*
 * Include-only, and it has to stay that way: this entity owns seven lazy collections, so an opt-out toString
 * loads the whole aggregate - every component, message, relation, REST API, spec and schema - from one log
 * line. See EntityToStringDoesNotQueryTest, which fails the build if any entity's toString reaches the
 * database.
 */
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Getter
@Slf4j
public class System extends MutableDomainEntity implements Persistable<UUID> {

    @Transient
    @EqualsAndHashCode.Exclude
    // Assigned UUIDs must not make a newly onboarded aggregate look detached to Spring Data.
    private boolean newEntity = true;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    private void markPersisted() {
        newEntity = false;
    }

    @Id
    @NotNull
    @ToString.Include
    private UUID id;

    @NotNull
    @ToString.Include
    private String name;

    private String description;

    private String confluenceLink;

    @Setter
    @ManyToOne(fetch = FetchType.EAGER)
    private Team defaultOwner;

    /**
     * Alias names under which this system is know, i.e. DataAnalytics also known als DA in Event prefixes.
     * Useful to import elements for systems which do not consistenly follow the naming conventions, or use
     * abbreviations in some contexts.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "system_aliases")
    private List<String> aliases = new ArrayList<>();

    /*
     * No orphan removal on the four collections whose elements can move to another system (components, and the
     * REST APIs, OpenAPI specs and database schemas that move with them, see moveSystemComponentTo): JPA forbids
     * reassigning an orphaned entity, and Hibernate deletes it or not depending on the order the systems were
     * loaded in. A removal is recorded instead (removedSystemComponents / removedArtifacts) and deleted explicitly
     * by ArchitectureModelRepository; a move is a plain foreign-key update. Deleting the whole system still cascades.
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent")
    private List<SystemComponent> systemComponents = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parent")
    private List<Event> events = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parent")
    private List<Command> commands = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "definingSystem")
    private List<RestApi> restApis = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "definingSystem")
    private List<AbstractRelation> relations = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "definingSystem")
    private List<OpenApiSpec> openApiSpecs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "system")
    private List<SystemComponentDatabaseSchema> databaseSchemas = new ArrayList<>();

    @Transient
    @EqualsAndHashCode.Exclude
    private final List<SystemComponent> removedSystemComponents = new ArrayList<>();

    @Transient
    @EqualsAndHashCode.Exclude
    private final List<MutableDomainEntity> removedArtifacts = new ArrayList<>();

    @SuppressWarnings("java:S107")
    private System(String name, String description, String confluenceLink, Team defaultOwner, List<String> aliases,
                   List<SystemComponent> systemComponents,
                   List<Event> events, List<Command> commands, List<RestApi> restApis, List<AbstractRelation> relations) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.confluenceLink = confluenceLink;
        this.defaultOwner = defaultOwner;
        this.aliases = new ArrayList<>(aliases);
        this.systemComponents = new ArrayList<>();
        systemComponents.forEach(this::addSystemComponent);
        this.events = new ArrayList<>();
        events.forEach(this::addEvent);
        this.commands = new ArrayList<>();
        commands.forEach(this::addCommand);
        this.restApis = new ArrayList<>();
        restApis.forEach(this::addRestApi);
        this.relations = new ArrayList<>();
        relations.forEach(this::addRelation);
    }

    public static SystemBuilder builder() {
        return new SystemBuilder();
    }

    public List<String> getAliases() {
        return unmodifiableList(aliases);
    }

    public void updateName(String name) {
        if (!this.name.equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("System renames are not supported: " + this.name + " -> " + name);
        }
        this.name = name;
    }

    public void updateDetails(String description, String confluenceLink, Team defaultOwner) {
        this.description = description;
        this.confluenceLink = confluenceLink;
        this.defaultOwner = defaultOwner;
    }

    public void replaceAliases(List<String> aliases) {
        if (!this.aliases.equals(aliases)) {
            this.aliases.clear();
            this.aliases.addAll(aliases);
        }
    }

    public List<Relation> getRelations() {
        return unmodifiableList(relations);
    }

    public List<Relation> getActiveRelations() {
        // Not Stream.toList(): the stream is of AbstractRelation, and toList() is not target-typed, so it
        // yields a List<AbstractRelation> that does not fit the List<Relation> returned here. The collector
        // is, and infers Relation as its element type. Sonar's java:S6204 does not apply here.
        @SuppressWarnings("java:S6204")
        List<Relation> activeRelations = relations.stream()
                .filter(r -> RelationStatus.ACTIVE.equals(r.getStatus()))
                .collect(Collectors.toUnmodifiableList());
        return activeRelations;
    }

    public List<Event> getEvents() {
        return unmodifiableList(events);
    }

    public List<Command> getCommands() {
        return unmodifiableList(commands);
    }

    public List<RestApi> getRestApis() {
        return unmodifiableList(restApis);
    }

    public List<OpenApiSpec> getOpenApiSpecs() {
        return unmodifiableList(openApiSpecs);
    }

    public List<SystemComponentDatabaseSchema> getDatabaseSchemas() {
        return unmodifiableList(databaseSchemas);
    }

    public Optional<SystemComponentDatabaseSchema> getDatabaseSchema(String systemComponentName) {
        return databaseSchemas.stream()
                .filter(componentSchema  ->
                        componentSchema.getSystemComponent().getName().equalsIgnoreCase(systemComponentName))
                .findFirst();
    }

    public void addSystemComponent(SystemComponent systemComponent) {
        if (systemComponents.stream().anyMatch(rc -> rc.getName().equalsIgnoreCase(systemComponent.getName()))) {
            throw new IllegalArgumentException("Cannot add duplicate system component " + systemComponent.getName());
        }

        systemComponent.setParent(this);
        systemComponents.add(systemComponent);
        sortSystemComponents();
    }

    public void sortSystemComponents() {
        systemComponents.sort(comparing(SystemComponent::getName));
    }

    /**
     * Moves every component of this system (including undeployed ones) onto {@code target}, each together with
     * its artifacts - see {@link #moveSystemComponentTo}. Used by onboarding when this system's name, or one of
     * its aliases, is reassigned to {@code target} as an alias - i.e. this system was renamed, not actually
     * removed. Without this, an importer resolves the alias to {@code target}, does not find the component there,
     * and recreates it, colliding with the still-present old row on the database's global unique index on the
     * component's name.
     * <p>
     * All or nothing: if {@code target} already has a component of one of the names, nothing is moved.
     */
    public void transferSystemComponentsTo(System target) {
        List<SystemComponent> moved = new ArrayList<>(systemComponents);
        moved.forEach(component -> target.requireNoSystemComponentNamed(component.getName()));
        moved.forEach(component -> moveSystemComponentTo(component, target));
    }

    /**
     * Moves {@code component} onto {@code target} together with what this system holds for it: the REST APIs it
     * provides, its OpenAPI spec and its database schema. The artifacts have to follow it - every reader (the docs
     * API, the Swagger link, the missing-spec report) looks them up on the component's current system, and a push
     * updates the existing row in place without ever moving it, so an artifact left behind would stay invisible
     * for good, and be deleted with this system. Relations referencing a moved REST API keep pointing at it.
     */
    public void moveSystemComponentTo(SystemComponent component, System target) {
        if (!systemComponents.contains(component)) {
            throw new IllegalArgumentException("System component " + component.getName() + " is not part of system " + name);
        }
        target.requireNoSystemComponentNamed(component.getName());
        systemComponents.remove(component);
        target.addSystemComponent(component);
        moveMatching(restApis, restApi -> component.equals(restApi.getProvider()), restApi -> {
            restApi.setDefiningSystem(target);
            target.addRestApi(restApi);
        });
        moveMatching(openApiSpecs, spec -> component.equals(spec.getProvider()), target::addOpenApiSpec);
        moveMatching(databaseSchemas, schema -> component.equals(schema.getSystemComponent()), schema -> {
            schema.setSystem(target);
            target.addDatabaseSchema(schema);
        });
    }

    private static <T> void moveMatching(List<T> source, Predicate<T> belongsToComponent, Consumer<T> addToTarget) {
        List<T> matching = source.stream().filter(belongsToComponent).toList();
        source.removeAll(matching);
        matching.forEach(addToTarget);
    }

    private void requireNoSystemComponentNamed(String componentName) {
        if (findSystemComponent(componentName).isPresent()) {
            throw new IllegalArgumentException("Cannot add duplicate system component " + componentName);
        }
    }

    public void addEvent(Event event) {
        if (events.stream().anyMatch(e -> e.getMessageTypeName().equalsIgnoreCase(event.getMessageTypeName()))) {
            throw new IllegalArgumentException("Cannot add duplicate event " + event.getMessageTypeName());
        }
        event.setParent(this);
        events.add(event);
        events.sort(comparing(Event::getMessageTypeName));
    }

    public void addCommand(Command command) {
        if (commands.stream().anyMatch(e -> e.getMessageTypeName().equalsIgnoreCase(command.getMessageTypeName()))) {
            throw new IllegalArgumentException("Cannot add duplicate command " + command.getMessageTypeName());
        }
        command.setParent(this);
        commands.add(command);
        commands.sort(comparing(Command::getMessageTypeName));
    }

    public void addRelation(AbstractRelation relation) {
        if (relations.contains(relation)) {
            throw new IllegalArgumentException("Cannot add duplicate relation " + relation);
        }
        relation.setDefiningSystem(this);
        relations.add(relation);
        relations.sort(comparing(Relation::getType).thenComparing(Relation::getLabel));
    }

    public void addOpenApiSpec(OpenApiSpec openApiSpec) {
        if (openApiSpecs.contains(openApiSpec)) {
            throw new IllegalArgumentException("Cannot add duplicate openApiSpec " + openApiSpec);
        }
        openApiSpec.setDefiningSystem(this);
        openApiSpecs.add(openApiSpec);
    }

    public void addDatabaseSchema(SystemComponentDatabaseSchema databaseSchema) {
        if (this != databaseSchema.getSystem()) {
            throw new IllegalArgumentException("Cannot add database schema to different system " +
                    this.getName() + ": " + databaseSchema);
        }
        if (databaseSchemas.contains(databaseSchema)) {
            throw new IllegalArgumentException("Database schema already present: " + databaseSchema);
        }
        databaseSchemas.add(databaseSchema);
    }

    public void addRestApi(RestApi restApi) {
        if (restApis.contains(restApi)) {
            throw new IllegalArgumentException("Cannot add duplicate REST API for provider " + restApi.getProvider());
        }
        restApis.add(restApi);
    }

    /**
     * The components of this system that are still deployed - what a page, a diagram, a relation list or either
     * REST API (docs or model) should show as the current architecture. This is the default view: readers get it
     * without having to remember to filter.
     */
    public List<SystemComponent> getSystemComponents() {
        return systemComponents.stream().filter(component -> !component.isUndeployed()).toList();
    }

    /**
     * All components of this system, including undeployed ones. An undeployed component is kept in the model only
     * for internal housekeeping, which is what lets an importer mark it undeployed again, or redeployed, rather
     * than recreate it. Only importers and the model's own bookkeeping should use this; nothing that reports the
     * architecture to a reader or another service may.
     */
    public List<SystemComponent> getSystemComponentsIncludingUndeployed() {
        return unmodifiableList(systemComponents);
    }

    public Optional<SystemComponent> findSystemComponent(String name) {
        return systemComponents.stream()
                .filter(systemComponent -> name.equalsIgnoreCase(systemComponent.getName()))
                .findFirst();
    }

    RemovedElements removeAllByImporter(Importer importer) {
        relations.removeAll(MultipleImportable.filterByImportedOnlyByImporter(relations, importer));
        removeArtifacts(restApis, MultipleImportable.filterByImportedOnlyByImporter(restApis, importer)::contains);

        Set<SystemComponentRemoved> systemComponentRemovedList = new HashSet<>();
        Set<EventRemoved> eventRemovedList = new HashSet<>();
        Set<CommandRemoved> commandRemovedList = new HashSet<>();

        filterByImporter(systemComponents, importer).forEach(sc -> systemComponentRemovedList.add(removeSystemComponent(sc)));
        filterByImporter(events, importer).forEach(e -> eventRemovedList.add(removeEvent(e)));
        filterByImporter(commands, importer).forEach(c -> commandRemovedList.add(removeCommand(c)));

        return new RemovedElements(systemComponentRemovedList, eventRemovedList, commandRemovedList);
    }

    SystemComponentRemoved removeSystemComponent(SystemComponent systemComponent) {
        if (systemComponents.remove(systemComponent)) {
            removedSystemComponents.add(systemComponent);
        }
        return SystemComponentRemoved.of(systemComponent);
    }

    public EventRemoved removeEvent(Event event) {
        events.remove(event);
        return EventRemoved.of(event);
    }

    public CommandRemoved removeCommand(Command command) {
        commands.remove(command);
        return CommandRemoved.of(command);
    }

    public void removeRestApiRelation(RestApiRelation restApiRelation) {
        relations.remove(restApiRelation);
    }

    public void removeRestApi(RestApi restApi) {
        removeArtifacts(restApis, restApi::equals);
    }

    /**
     * The components removed from this system that still have to be deleted - see the comment on
     * {@link #systemComponents} for why they are not left to orphan removal.
     */
    public List<SystemComponent> getRemovedSystemComponents() {
        return unmodifiableList(removedSystemComponents);
    }

    /** The REST APIs, OpenAPI specs and database schemas removed from this system that still have to be deleted. */
    public List<MutableDomainEntity> getRemovedArtifacts() {
        return unmodifiableList(removedArtifacts);
    }

    /**
     * Brings back a component removed from this system earlier in the same update, onto {@code target}, instead of
     * having a new one of the same name created - which would collide with the old row on the global unique index
     * of the component name, as Hibernate inserts before it deletes. Its artifacts stay removed.
     */
    Optional<SystemComponent> reviveRemovedSystemComponent(String componentName, System target) {
        Optional<SystemComponent> removed = removedSystemComponents.stream()
                .filter(component -> component.getName().equalsIgnoreCase(componentName))
                .findFirst();
        removed.ifPresent(component -> {
            target.requireNoSystemComponentNamed(component.getName());
            removedSystemComponents.remove(component);
            target.addSystemComponent(component);
        });
        return removed;
    }

    private <T extends MutableDomainEntity> void removeArtifacts(List<T> artifacts, Predicate<T> toRemove) {
        List<T> removed = artifacts.stream().filter(toRemove).toList();
        artifacts.removeAll(removed);
        removedArtifacts.addAll(removed);
    }


    /**
     * Remove relations if the consumer or provider has been removed
     */
    void onSystemComponentRemoved(SystemComponentRemoved event) {
        String removedSystemComponentName = event.getSystemComponentName();
        relations.removeIf(rel -> removedSystemComponentName.equals(rel.getProviderName()) || removedSystemComponentName.equals(rel.getConsumerName()));
        removeArtifacts(restApis, restApi -> restApi.getProvider().getName().equals(removedSystemComponentName));
        removeArtifacts(openApiSpecs, openApiSpec -> openApiSpec.getProvider().getName().equals(removedSystemComponentName));
        removeArtifacts(databaseSchemas, dbSchema -> dbSchema.getSystemComponent().getName().equals(removedSystemComponentName));
    }

    /**
     * Remove event relations if the underlying event has been removed
     */
    void onEventRemoved(EventRemoved event) {
        String eventName = event.getEventName();
        relations.removeIf(rel ->
                (rel instanceof EventRelation er) && eventName.equals(er.getEventName()));
    }

    /**
     * Remove command relations if the underlying command has been removed
     */
    void onCommandRemoved(CommandRemoved event) {
        String commandName = event.getCommandName();
        relations.removeIf(rel ->
                (rel instanceof CommandRelation cr) && commandName.equals(cr.getCommandName()));
    }

    public Optional<MessageType> findMessageType(String messageTypeName) {
        return Stream.concat(events.stream(), commands.stream())
                .filter(messageType -> messageType.getMessageTypeName().equalsIgnoreCase(messageTypeName))
                .findFirst();
    }

    public static class SystemBuilder {
        private String name;
        private String description;
        private String confluenceLink;
        private Team defaultOwner;
        private List<String> aliases = new ArrayList<>();
        private List<SystemComponent> systemComponents = new ArrayList<>();
        private List<Event> events = new ArrayList<>();
        private List<Command> commands = new ArrayList<>();
        private List<RestApi> restApis = new ArrayList<>();
        private List<AbstractRelation> relations = new ArrayList<>();

        SystemBuilder() {
        }

        public SystemBuilder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        public SystemBuilder description(String description) {
            this.description = description;
            return this;
        }

        public SystemBuilder confluenceLink(String confluenceLink) {
            this.confluenceLink = confluenceLink;
            return this;
        }

        public SystemBuilder defaultOwner(Team defaultOwner) {
            this.defaultOwner = defaultOwner;
            return this;
        }

        public SystemBuilder aliases(List<String> aliases) {
            this.aliases = aliases;
            return this;
        }

        public SystemBuilder systemComponents(List<SystemComponent> systemComponents) {
            this.systemComponents = systemComponents;
            return this;
        }

        public SystemBuilder events(List<Event> events) {
            this.events = events;
            return this;
        }

        public SystemBuilder commands(List<Command> commands) {
            this.commands = commands;
            return this;
        }

        public SystemBuilder restApis(List<RestApi> restApis) {
            this.restApis = restApis;
            return this;
        }

        public SystemBuilder relations(List<AbstractRelation> relations) {
            this.relations = relations;
            return this;
        }

        public System build() {
            return new System(name, description, confluenceLink, defaultOwner, aliases, systemComponents, events, commands, restApis, relations);
        }
    }
}
