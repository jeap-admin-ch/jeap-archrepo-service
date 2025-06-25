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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.admin.bit.jeap.archrepo.metamodel.Importable.filterByImporter;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;

@SuppressWarnings({"findbugs:RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "java:S1144"})
// Sonar does not always play well with lombok
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
@Entity
@Getter
@Slf4j
public class System extends MutableDomainEntity {

    @Id
    @NotNull
    private UUID id;

    @NotNull
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parent")
    private List<SystemComponent> systemComponents = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parent")
    private List<Event> events = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "parent")
    private List<Command> commands = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "definingSystem")
    private List<RestApi> restApis = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "definingSystem")
    private List<AbstractRelation> relations = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "definingSystem")
    private List<OpenApiSpec> openApiSpecs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "system")
    private List<SystemComponentDatabaseSchema> databaseSchemas = new ArrayList<>();

    @SuppressWarnings("java:S107")
    private System(String name, String description, String confluenceLink, Team defaultOwner, List<String> aliases,
                   List<SystemComponent> systemComponents,
                   List<Event> events, List<Command> commands, List<RestApi> restApis, List<AbstractRelation> relations) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.confluenceLink = confluenceLink;
        this.defaultOwner = defaultOwner;
        this.aliases = aliases;
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

    public List<Relation> getRelations() {
        return unmodifiableList(relations);
    }

    public List<Relation> getActiveRelations() {
        return relations.stream().filter(r -> RelationStatus.ACTIVE.equals(r.getStatus())).collect(Collectors.toUnmodifiableList());
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

    public List<SystemComponent> getSystemComponents() {
        return unmodifiableList(systemComponents);
    }

    public Optional<SystemComponent> findSystemComponent(String name) {
        return systemComponents.stream()
                .filter(systemComponent -> name.equalsIgnoreCase(systemComponent.getName()))
                .findFirst();
    }

    RemovedElements removeAllByImporter(Importer importer) {
        relations.removeAll(MultipleImportable.filterByImportedOnlyByImporter(relations, importer));
        restApis.removeAll(MultipleImportable.filterByImportedOnlyByImporter(restApis, importer));

        Set<SystemComponentRemoved> systemComponentRemovedList = new HashSet<>();
        Set<EventRemoved> eventRemovedList = new HashSet<>();
        Set<CommandRemoved> commandRemovedList = new HashSet<>();

        filterByImporter(systemComponents, importer).forEach(sc -> systemComponentRemovedList.add(removeSystemComponent(sc)));
        filterByImporter(events, importer).forEach(e -> eventRemovedList.add(removeEvent(e)));
        filterByImporter(commands, importer).forEach(c -> commandRemovedList.add(removeCommand(c)));

        return new RemovedElements(systemComponentRemovedList, eventRemovedList, commandRemovedList);
    }

    SystemComponentRemoved removeSystemComponent(SystemComponent systemComponent) {
        systemComponents.remove(systemComponent);
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
        restApis.remove(restApi);
    }


    /**
     * Remove relations if the consumer or provider has been removed
     */
    void onSystemComponentRemoved(SystemComponentRemoved event) {
        String removedSystemComponentName = event.getSystemComponentName();
        relations.removeIf(rel -> removedSystemComponentName.equals(rel.getProviderName()) || removedSystemComponentName.equals(rel.getConsumerName()));
        restApis.removeIf(restApi -> restApi.getProvider().getName().equals(removedSystemComponentName));
        openApiSpecs.removeIf(openApiSpec -> openApiSpec.getProvider().getName().equals(removedSystemComponentName));
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
