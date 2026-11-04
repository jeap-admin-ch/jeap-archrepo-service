package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.web.ArchRepoModelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static ch.admin.bit.jeap.archrepo.web.ModelNames.index;
import static ch.admin.bit.jeap.archrepo.web.ModelNames.key;
import static ch.admin.bit.jeap.archrepo.web.ModelNames.validateNamespace;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelOnboardingService {
    private final ArchRepoModelProperties properties;

    public void apply(ArchitectureModel model) {
        if (properties.teams().isEmpty() || properties.systems().isEmpty()) {
            log.warn("Model onboarding is inactive: both archrepo.model.teams and archrepo.model.systems must be non-empty");
            return;
        }
        try {
            reconcile(model);
        } catch (IllegalArgumentException e) {
            log.error("Invalid model onboarding configuration: {}", e.getMessage());
            throw e;
        }
    }

    private void reconcile(ArchitectureModel model) {
        var teams = index(properties.teams(), ArchRepoModelProperties.TeamDefinition::name, "configured team");
        var systems = index(properties.systems(), ArchRepoModelProperties.SystemDefinition::name, "configured system");
        var existingTeams = index(model.getTeams(), Team::getName, "stored team");
        var existingSystems = index(model.getSystems(), System::getName, "stored system");
        validateAgainstStoredModel(model, systems, existingSystems);

        var teamCounts = reconcileTeams(model, teams, existingTeams);
        var systemCounts = reconcileSystems(model, systems, existingSystems, existingTeams);
        int removedSystems = removeObsoleteSystems(model, systems, existingSystems);
        int removedTeams = removeObsoleteTeams(model, teams);
        log.info("Model onboarding: systems created={}, updated={}, deleted={}; teams created={}, updated={}, deleted={}",
                systemCounts.created(), systemCounts.updated(), removedSystems,
                teamCounts.created(), teamCounts.updated(), removedTeams);
    }

    /**
     * The checks that need the stored model. Everything that can be checked on the declaration alone - duplicate
     * names, conflicting aliases, undeclared teams - is rejected when {@link ArchRepoModelProperties} is bound,
     * so a broken document fails the startup rather than this update.
     */
    private static void validateAgainstStoredModel(ArchitectureModel model,
                                                   Map<String, ArchRepoModelProperties.SystemDefinition> systems,
                                                   Map<String, System> existingSystems) {
        // Legacy stored aliases may repeat their own system's name; configuration remains strictly validated.
        validateNamespace(model.getSystems(), System::getName,
                system -> system.getAliases().stream()
                        .filter(alias -> !alias.equalsIgnoreCase(system.getName())).toList(), "stored");
        validateRetainedNamespace(systems, existingSystems);
        existingSystems.forEach((name, stored) -> {
            if (!systems.containsKey(name)) {
                renameClaimants(stored, systems);
            }
        });
    }

    private static ReconciliationCounts reconcileTeams(ArchitectureModel model,
                                                       Map<String, ArchRepoModelProperties.TeamDefinition> teams,
                                                       Map<String, Team> existingTeams) {
        int createdTeams = 0;
        int updatedTeams = 0;
        for (var definition : teams.values()) {
            Team team = existingTeams.get(key(definition.name()));
            if (team == null) {
                team = Team.builder().name(definition.name()).build();
                model.addTeam(team);
                existingTeams.put(key(definition.name()), team);
                createdTeams++;
            } else if (!Objects.equals(team.getName(), definition.name())
                    || !Objects.equals(team.getContactAddress(), definition.contactAddress())
                    || !Objects.equals(team.getConfluenceLink(), definition.confluenceLink())
                    || !Objects.equals(team.getJiraLink(), definition.jiraLink())) {
                updatedTeams++;
            }
            team.updateName(definition.name());
            team.updateDetails(definition.contactAddress(), definition.confluenceLink(), definition.jiraLink());
        }
        return new ReconciliationCounts(createdTeams, updatedTeams);
    }

    private static ReconciliationCounts reconcileSystems(ArchitectureModel model,
                                                         Map<String, ArchRepoModelProperties.SystemDefinition> systems,
                                                         Map<String, System> existingSystems,
                                                         Map<String, Team> existingTeams) {
        int createdSystems = 0;
        int updatedSystems = 0;
        for (var definition : systems.values()) {
            System system = existingSystems.get(key(definition.name()));
            Team owner = existingTeams.get(key(definition.team()));
            if (system == null) {
                system = System.builder().name(definition.name()).build();
                model.addSystem(system);
                existingSystems.put(key(definition.name()), system);
                createdSystems++;
            } else if (!Objects.equals(system.getName(), definition.name())
                    || !Objects.equals(system.getDescription(), definition.description())
                    || !Objects.equals(system.getConfluenceLink(), definition.confluenceLink())
                    || !Objects.equals(system.getDefaultOwner(), owner)
                    || !Objects.equals(system.getAliases(), definition.aliases())) {
                updatedSystems++;
            }
            system.updateName(definition.name());
            system.updateDetails(definition.description(), definition.confluenceLink(),
                    owner);
            system.replaceAliases(definition.aliases());
        }
        return new ReconciliationCounts(createdSystems, updatedSystems);
    }

    private record ReconciliationCounts(int created, int updated) {
    }

    private static int removeObsoleteSystems(ArchitectureModel model,
                                            Map<String, ArchRepoModelProperties.SystemDefinition> systems,
                                            Map<String, System> existingSystems) {
        var removedSystems = model.getSystems().stream()
                .filter(system -> !systems.containsKey(key(system.getName()))).toList();
        removedSystems.forEach(removed -> renameTarget(removed, systems, existingSystems)
                .ifPresent(removed::transferSystemComponentsTo));
        removedSystems.forEach(model::removeSystem);
        return removedSystems.size();
    }

    private static int removeObsoleteTeams(ArchitectureModel model,
                                          Map<String, ArchRepoModelProperties.TeamDefinition> teams) {
        var removedTeams = model.getTeams().stream()
                .filter(team -> !teams.containsKey(key(team.getName()))).toList();
        for (System system : model.getSystems()) {
            system.getSystemComponentsIncludingUndeployed().stream()
                    .filter(component -> removedTeams.contains(component.getOwnedBy()))
                    .forEach(component -> component.setOwnedBy(system.getDefaultOwner()));
        }
        removedTeams.forEach(model::removeTeam);
        return removedTeams.size();
    }

    /**
     * The surviving system a removed system's identity continues as: the configured system that claims - as its
     * name or as one of its aliases - the removed system's current name or one of its current aliases. Names and
     * aliases share one namespace, so taking over the old alias as the new name is a rename just as listing the old
     * name as an alias is. Absent, this is a plain deletion.
     * <p>
     * {@link ArchRepoModelProperties} guarantees that no name or alias is claimed twice, but two systems may still
     * claim different parts of the same removed system - one its name, the other an alias. That rename is ambiguous
     * and rejected, rather than letting the order of the declaration pick where the components go.
     */
    private static Optional<System> renameTarget(System removed, Map<String, ArchRepoModelProperties.SystemDefinition> systems,
                                                   Map<String, System> existingSystems) {
        return renameClaimants(removed, systems).stream().findFirst()
                .map(definition -> existingSystems.get(key(definition.name())));
    }

    /** The configured systems claiming part of the removed system's identity - at most one, see above. */
    private static List<ArchRepoModelProperties.SystemDefinition> renameClaimants(
            System removed, Map<String, ArchRepoModelProperties.SystemDefinition> systems) {
        var removedNames = new ArrayList<>(removed.getAliases());
        removedNames.add(removed.getName());
        var claimants = systems.values().stream()
                .filter(definition -> removedNames.stream().anyMatch(name -> claims(definition, name)))
                .toList();
        if (claimants.size() > 1) {
            throw new IllegalArgumentException("Ambiguous rename of system '%s' (names/aliases %s): claimed by %s"
                    .formatted(removed.getName(), removedNames,
                            claimants.stream().map(ArchRepoModelProperties.SystemDefinition::name).toList()));
        }
        return claimants;
    }

    private static boolean claims(ArchRepoModelProperties.SystemDefinition definition, String name) {
        return definition.name().equalsIgnoreCase(name) || containsIgnoreCase(definition.aliases(), name);
    }

    private static boolean containsIgnoreCase(List<String> values, String target) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(target));
    }

    private static void validateRetainedNamespace(Map<String, ArchRepoModelProperties.SystemDefinition> desired,
                                                  Map<String, System> existing) {
        Map<String, String> retained = new LinkedHashMap<>();
        existing.forEach((name, system) -> {
            if (desired.containsKey(name)) {
                retained.put(name, system.getName());
                system.getAliases().forEach(alias -> retained.put(key(alias), system.getName()));
            }
        });
        for (var definition : desired.values()) {
            var names = new ArrayList<>(definition.aliases());
            names.add(definition.name());
            for (String name : names) {
                String owner = retained.get(key(name));
                if (owner != null && !key(owner).equals(key(definition.name()))) {
                    throw new IllegalArgumentException("Name/alias '%s' of system '%s' conflicts with remaining system '%s'"
                            .formatted(name, definition.name(), owner));
                }
            }
        }
    }

}
