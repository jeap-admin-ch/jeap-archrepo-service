package ch.admin.bit.jeap.archrepo.metamodel;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;

@UtilityClass
@Slf4j
public class ArchitectureModelHelper {

    public static Optional<SystemComponent> findComponentByNameWithSystemPrefix(ArchitectureModel model, String name, Pattern nameParts) {
        Matcher matcher = nameParts.matcher(name);
        if (matcher.matches()) {
            String systemName = matcher.group("system");
            String componentName = systemName + "-" + matcher.group("component");
            return model.findSystem(systemName)
                    .flatMap(system -> system.findSystemComponent(componentName))
                    .or(() -> model.findSystemComponent(componentName));
        }
        return Optional.empty();
    }

    /**
     * @see ArchitectureModel#cleanup()
     */
    static void cleanup(ArchitectureModel model) {
        removeOutdatedImportedRestRelations(model);
        removeNotReferencedRestApis(model);
    }


    private static void removeOutdatedImportedRestRelations(ArchitectureModel model) {
        ZonedDateTime deleteBeforeDate = ZonedDateTime.now().minusMonths(3);

        model.getAllRelationsByType(RestApiRelation.class).stream()
                .filter(ArchitectureModelHelper::isImported)
                .filter(lastSeenBefore(deleteBeforeDate))
                .forEach(restApiRelation -> restApiRelation.getDefiningSystem().removeRestApiRelation(restApiRelation));
    }

    private static void removeNotReferencedRestApis(ArchitectureModel model) {
        Set<UUID> allRestApiIds = model.getAllRelationsByType(RestApiRelation.class).stream()
                .map(relation -> relation.getRestApi().getId()).collect(toSet());

        model.getAllRestApis().stream()
                .filter(restApi -> !allRestApiIds.contains(restApi.getId()))
                .forEach(restApi -> restApi.getDefiningSystem().removeRestApi(restApi));
    }

    private static Predicate<RestApiRelation> lastSeenBefore(ZonedDateTime deleteBeforeDate) {
        return restApiRelation -> (restApiRelation.getLastSeen() == null) || (restApiRelation.getLastSeen().isBefore(deleteBeforeDate));
    }

    private static boolean isImported(MultipleImportable importable) {
        return !importable.getImporters().isEmpty();
    }
}
