package ch.admin.bit.jeap.archrepo.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "archrepo.model", ignoreUnknownFields = false)
public record ArchRepoModelProperties(List<@NotNull @Valid TeamDefinition> teams,
                                     List<@NotNull @Valid SystemDefinition> systems) {
    /**
     * Rejects a declaration that is inconsistent in itself - duplicate names, a name or alias claimed twice, a
     * system referencing an undeclared team - while it is bound, so that a broken document fails the startup
     * instead of every later model update. None of this needs the database; conflicts with stored data are left
     * to onboarding. An inactive document (either list empty) is not checked, as onboarding ignores it too.
     */
    public ArchRepoModelProperties {
        teams = teams == null ? List.of() : List.copyOf(teams);
        systems = systems == null ? List.of() : List.copyOf(systems);
        if (!teams.isEmpty() && !systems.isEmpty() && hasAllNames(teams, systems)) {
            validateDeclaration(teams, systems);
        }
    }

    /** Missing or blank names are reported by bean validation, which runs after construction. */
    private static boolean hasAllNames(List<TeamDefinition> teams, List<SystemDefinition> systems) {
        return teams.stream().allMatch(team -> team != null && hasText(team.name()))
                && systems.stream().allMatch(system -> system != null && hasText(system.name())
                && hasText(system.team()) && system.aliases().stream().allMatch(ArchRepoModelProperties::hasText));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void validateDeclaration(List<TeamDefinition> teams, List<SystemDefinition> systems) {
        var declaredTeams = ModelNames.index(teams, TeamDefinition::name, "configured team");
        ModelNames.index(systems, SystemDefinition::name, "configured system");
        ModelNames.validateNamespace(systems, SystemDefinition::name, SystemDefinition::aliases, "configured");
        for (var system : systems) {
            if (!declaredTeams.containsKey(ModelNames.key(system.team()))) {
                throw new IllegalArgumentException("System '%s' references undeclared team '%s'"
                        .formatted(system.name(), system.team()));
            }
        }
    }

    public record TeamDefinition(@NotBlank String name, String contactAddress,
                                 String confluenceLink, String jiraLink) {
    }

    public record SystemDefinition(@NotBlank String name, @NotBlank String team,
                                   @Size(max = 1024) String description, String confluenceLink,
                                   List<@NotBlank String> aliases) {
        public SystemDefinition {
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
        }
    }
}
