package ch.admin.bit.jeap.archrepo.web;

import ch.admin.bit.jeap.archrepo.web.ArchRepoModelProperties.SystemDefinition;
import ch.admin.bit.jeap.archrepo.web.ArchRepoModelProperties.TeamDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchRepoModelPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void missingConfigurationBindsToEmptyLists() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ArchRepoModelProperties.class).systems()).isEmpty();
            assertThat(context.getBean(ArchRepoModelProperties.class).teams()).isEmpty();
        });
    }

    @Test
    void bindsHyphenatedFieldsAndAcceptsSchemaLimits() {
        runner.withPropertyValues("archrepo.model.teams[0].name=Team Blue",
                "archrepo.model.teams[0].contact-address=" + "a".repeat(2000),
                "archrepo.model.teams[0].confluence-link=" + "b".repeat(2000),
                "archrepo.model.teams[0].jira-link=" + "c".repeat(2000),
                "archrepo.model.systems[0].name=" + "d".repeat(2000),
                "archrepo.model.systems[0].team=Team Blue",
                "archrepo.model.systems[0].confluence-link=" + "e".repeat(2000),
                "archrepo.model.systems[0].aliases[0]=" + "f".repeat(2000),
                "archrepo.model.systems[0].description=" + "x".repeat(1024))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(ArchRepoModelProperties.class);
                    assertThat(properties.teams().getFirst().contactAddress()).hasSize(2000);
                    assertThat(properties.systems().getFirst().aliases().getFirst()).hasSize(2000);
                    assertThat(properties.systems().getFirst().description()).hasSize(1024);
                });
    }

    @Test
    void rejectsUnknownFieldsAndInvalidNestedValuesAtStartup() {
        for (String invalid : new String[]{
                "archrepo.model.typo=true",
                "archrepo.model.systems[0].typo=true",
                "archrepo.model.systems[0].name= ",
                "archrepo.model.systems[0].team= ",
                "archrepo.model.teams[0].name= ",
                "archrepo.model.systems[0].aliases[0]= ",
                "archrepo.model.systems[0].description=" + "x".repeat(1025)}) {
            runner.withPropertyValues("archrepo.model.teams[0].name=blue",
                            "archrepo.model.systems[0].name=foo", "archrepo.model.systems[0].team=blue", invalid)
                    .run(context -> assertThat(context).as(invalid).hasFailed());
        }
    }

    @Test
    void rejectsDuplicateNamesAliasCollisionsAndUndeclaredTeamsWithoutDatabaseAccess() {
        assertInvalid(List.of(team("blue"), team("BLUE")), List.of(system("foo", "blue")), "blue", "BLUE");
        assertInvalid(List.of(team("blue")), List.of(system("foo", "blue"), system("FOO", "blue")), "foo", "FOO");
        assertInvalid(List.of(team("blue")), List.of(system("foo", "blue", "FOO")), "FOO", "foo");
        assertInvalid(List.of(team("blue")), List.of(system("foo", "blue", "a", "A")), "A", "foo");
        assertInvalid(List.of(team("blue")), List.of(system("bar", "blue"),
                system("foo", "blue", "BAR")), "BAR", "bar", "foo");
        assertInvalid(List.of(team("blue")), List.of(
                system("foo", "blue", "shared"),
                system("bar", "blue", "SHARED")), "SHARED", "bar", "foo");
        assertInvalid(List.of(team("blue")), List.of(system("foo", "blue"), system("bar", "missing")),
                "bar", "missing");
    }

    @Test
    void undeclaredTeamFailsTheStartup() {
        runner.withPropertyValues("archrepo.model.teams[0].name=blue",
                        "archrepo.model.systems[0].name=foo", "archrepo.model.systems[0].team=red")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .rootCause().hasMessageContaining("foo").hasMessageContaining("red"));
    }

    @Test
    void namespaceCollisionFailsTheStartup() {
        runner.withPropertyValues("archrepo.model.teams[0].name=blue",
                        "archrepo.model.systems[0].name=foo", "archrepo.model.systems[0].team=blue",
                        "archrepo.model.systems[1].name=bar", "archrepo.model.systems[1].team=blue",
                        "archrepo.model.systems[1].aliases[0]=FOO")
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .rootCause().hasMessageContaining("FOO").hasMessageContaining("bar"));
    }

    @Test
    void inactivePartialConfigurationIsNotCrossChecked() {
        // Onboarding ignores a document with an empty list, so it must not stop the service from starting either.
        assertThatCode(() -> new ArchRepoModelProperties(List.of(team("duplicate"), team("DUPLICATE")), null))
                .doesNotThrowAnyException();
        assertThatCode(() -> new ArchRepoModelProperties(null, List.of(system("foo", "unknown"),
                system("FOO", "unknown")))).doesNotThrowAnyException();
    }

    private static void assertInvalid(List<TeamDefinition> teams, List<SystemDefinition> systems, String... message) {
        assertThatThrownBy(() -> new ArchRepoModelProperties(teams, systems))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContainingAll(message);
    }

    private static TeamDefinition team(String name) {
        return new TeamDefinition(name, null, null, null);
    }

    private static SystemDefinition system(String name, String team, String... aliases) {
        return new SystemDefinition(name, team, null, null, List.of(aliases));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ArchRepoModelProperties.class)
    static class PropertiesConfiguration {
    }
}
