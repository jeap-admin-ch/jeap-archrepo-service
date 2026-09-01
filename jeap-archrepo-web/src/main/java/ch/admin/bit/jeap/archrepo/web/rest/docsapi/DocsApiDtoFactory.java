package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Relation;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageContract;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageVersion;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageVersionOrder;
import ch.admin.bit.jeap.archrepo.metamodel.relation.CommandRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.EventRelation;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.OpenApiSpec;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

/**
 * Maps the architecture model to the payloads of the docs API.
 * <p>
 * Deliberately separate from {@code ModelDtoFactory}, which serves {@code /api/model}: that payload is under Pact
 * contract with consumers this repository cannot enumerate, so every field the documentation needs would be a
 * change to a contracted payload. The duplicated relation mapping is the price of that segregation.
 */
@Component
class DocsApiDtoFactory {

    SystemListDto createSystemList(ArchitectureModel model) {
        return new SystemListDto(model.getSystems().stream()
                .sorted(comparing(System::getName))
                .map(this::createSystemSummary)
                .toList());
    }

    private SystemSummaryDto createSystemSummary(System system) {
        return new SystemSummaryDto(
                system.getName(),
                system.getDescription(),
                sorted(system.getAliases()),
                createTeam(system.getDefaultOwner()));
    }

    SystemDetailDto createSystemDetail(ArchitectureModel model, System system) {
        return new SystemDetailDto(
                system.getName(),
                system.getDescription(),
                sorted(system.getAliases()),
                createTeam(system.getDefaultOwner()),
                system.getSystemComponents().stream()
                        .sorted(comparing(SystemComponent::getName))
                        .map(component -> createComponent(model, system, component))
                        .toList(),
                createRelations(model, system));
    }

    private TeamDto createTeam(Team team) {
        return team == null ? null
                : new TeamDto(team.getName(), team.getContactAddress(), team.getJiraLink(), team.getConfluenceLink());
    }

    private ComponentDto createComponent(ArchitectureModel model, System system, SystemComponent component) {
        // A component without its own team is owned by the system's default owner - the generated page should
        // always name an owner where one is knowable.
        Team team = component.getOwnedBy() != null ? component.getOwnedBy() : system.getDefaultOwner();
        return new ComponentDto(
                component.getName(),
                component.getDescription(),
                component.getType(),
                createTeam(team),
                component.getImporter(),
                component.getLastSeen(),
                createRestApis(system, component),
                createOpenApiRef(model, system, component),
                createDatabaseSchemaRef(system, component));
    }

    private List<RestApiDto> createRestApis(System system, SystemComponent component) {
        return system.getRestApis().stream()
                .filter(restApi -> restApi.getProvider().equals(component))
                .map(restApi -> new RestApiDto(restApi.getMethod(), restApi.getPath()))
                .sorted(comparing(RestApiDto::path).thenComparing(RestApiDto::method))
                .toList();
    }

    private OpenApiRefDto createOpenApiRef(ArchitectureModel model, System system, SystemComponent component) {
        return findOpenApiSpec(system, component)
                .map(spec -> new OpenApiRefDto(
                        spec.getVersion(),
                        spec.getServerUrl(),
                        DocsApiPaths.openApiContentPath(system.getName(), component.getName()),
                        model.createOpenApiSpecSwaggerUrl(component)))
                .orElse(null);
    }

    private Optional<OpenApiSpec> findOpenApiSpec(System system, SystemComponent component) {
        return system.getOpenApiSpecs().stream()
                .filter(spec -> spec.getProvider().equals(component))
                .findFirst();
    }


    private DatabaseSchemaRefDto createDatabaseSchemaRef(System system, SystemComponent component) {
        return system.getDatabaseSchemas().stream()
                .filter(schema -> schema.getSystemComponent().equals(component))
                .findFirst()
                .map(schema -> new DatabaseSchemaRefDto(
                        schema.getSchemaVersion(),
                        DocsApiPaths.databaseSchemaContentPath(system.getName(), component.getName())))
                .orElse(null);
    }

    private List<RelationDto> createRelations(ArchitectureModel model, System system) {
        Map<String, String> componentToSystem = model.getAllSystemComponentNamesWithSystemName();
        return model.getSystems().stream()
                .flatMap(s -> s.getActiveRelations().stream())
                .filter(relation -> belongsToSystem(relation, system.getName(), componentToSystem))
                .map(relation -> createRelation(componentToSystem, relation))
                .sorted(comparing(RelationDto::type)
                        .thenComparing(RelationDto::consumer, nullsFirst(naturalOrder()))
                        .thenComparing(RelationDto::provider, nullsFirst(naturalOrder()))
                        .thenComparing(RelationDto::method, nullsFirst(naturalOrder()))
                        .thenComparing(RelationDto::path, nullsFirst(naturalOrder()))
                        .thenComparing(RelationDto::messageType, nullsFirst(naturalOrder())))
                .toList();
    }

    private boolean belongsToSystem(Relation relation, String systemName, Map<String, String> componentToSystem) {
        return systemName.equalsIgnoreCase(componentToSystem.get(relation.getConsumerName()))
               || systemName.equalsIgnoreCase(componentToSystem.get(relation.getProviderName()));
    }

    private RelationDto createRelation(Map<String, String> componentToSystem, Relation relation) {
        String method = null;
        String path = null;
        String pactUrl = null;
        String messageType = null;
        switch (relation) {
            case RestApiRelation restApiRelation -> {
                method = restApiRelation.getRestApi().getMethod();
                path = restApiRelation.getRestApi().getPath();
                pactUrl = restApiRelation.getPactUrl();
            }
            case EventRelation eventRelation -> messageType = eventRelation.getEventName();
            case CommandRelation commandRelation -> messageType = commandRelation.getCommandName();
            default -> {
                // A relation type without extra attributes; the common fields below are all there is
            }
        }
        return new RelationDto(
                relation.getType(),
                componentToSystem.get(relation.getConsumerName()),
                relation.getConsumerName(),
                componentToSystem.get(relation.getProviderName()),
                relation.getProviderName(),
                method,
                path,
                pactUrl,
                messageType);
    }

    MessageListDto createMessageList(ArchitectureModel model, System system) {
        Map<String, String> componentToSystem = model.getAllSystemComponentNamesWithSystemName();
        List<MessageDto> messages = Stream.concat(
                        system.getEvents().stream().map(event -> createMessage(componentToSystem, event)),
                        system.getCommands().stream().map(command -> createMessage(componentToSystem, command)))
                .sorted(comparing(MessageDto::name))
                .toList();
        return new MessageListDto(messages);
    }

    private MessageDto createMessage(Map<String, String> componentToSystem, MessageType messageType) {
        return new MessageDto(
                messageType.getMessageTypeName(),
                messageType instanceof Event ? "EVENT" : "COMMAND",
                messageType.getScope(),
                messageType.getTopic(),
                messageType.getDescriptorUrl(),
                messageType.getDocumentationUrl(),
                messageType.getDescription(),
                // The same order the message type index uses: two orders for one fact would be worse
                // than either, and a version list is read by people
                messageType.getMessageVersions().stream()
                        .map(MessageVersion::getVersion)
                        .sorted(MessageVersionOrder.INSTANCE)
                        .toList(),
                createContracts(componentToSystem, messageType));
    }

    private List<MessageContractDto> createContracts(Map<String, String> componentToSystem, MessageType messageType) {
        return switch (messageType) {
            case Event event -> Stream.concat(
                            event.getPublisherContracts().stream()
                                    .map(contract -> createContract(componentToSystem, "PUBLISHER", contract)),
                            event.getConsumerContracts().stream()
                                    .map(contract -> createContract(componentToSystem, "CONSUMER", contract)))
                    .sorted(contractOrder())
                    .toList();
            case Command command -> Stream.concat(
                            command.getSenderContracts().stream()
                                    .map(contract -> createContract(componentToSystem, "SENDER", contract)),
                            command.getReceiverContracts().stream()
                                    .map(contract -> createContract(componentToSystem, "RECEIVER", contract)))
                    .sorted(contractOrder())
                    .toList();
            default -> List.of();
        };
    }

    private static Comparator<MessageContractDto> contractOrder() {
        return Comparator.comparing(MessageContractDto::role)
                .thenComparing(MessageContractDto::component, nullsFirst(naturalOrder()));
    }

    private static List<String> sorted(List<String> values) {
        return values.stream().sorted().toList();
    }

    private MessageContractDto createContract(Map<String, String> componentToSystem, String role,
                                              MessageContract contract) {
        return new MessageContractDto(
                role,
                contract.getComponentName(),
                componentToSystem.get(contract.getComponentName()),
                contract.getTopic(),
                contract.versionList());
    }

}
