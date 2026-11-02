package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponentFactory;
import ch.admin.bit.jeap.archrepo.persistence.ArchitectureModelRepository;
import ch.admin.bit.jeap.archrepo.web.ArchRepoModelProperties;
import ch.admin.bit.jeap.archrepo.web.config.WebSecurityConfig;
import ch.admin.bit.jeap.archrepo.web.service.ModelOnboardingService;
import ch.admin.bit.jeap.security.test.resource.configuration.DisableJeapPermitAllSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModelExportController.class)
@Import({WebSecurityConfig.class, DisableJeapPermitAllSecurityConfiguration.class})
@ActiveProfiles("test")
class ModelExportControllerTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    ArchitectureModelRepository repository;
    @MockitoBean
    PlatformTransactionManager platformTransactionManager;

    @Test
    void exportIsPublicAndRoundTripsThroughSpringBinderRegardlessOfListOrder() throws Exception {
        Team blue = Team.builder().name("Team Blue").contactAddress("blue@example.ch")
                .confluenceLink("https://example.ch/team").jiraLink("https://example.ch/jira").build();
        Team unused = Team.builder().name("Unused").build();
        System foo = System.builder().name("foo").description("Multi-line\nUnicode: ä\non: true")
                .defaultOwner(blue).confluenceLink("https://example.ch/foo").aliases(List.of("z", "a")).build();
        var undeployed = SystemComponentFactory.createSystemComponent(foo, "foo-service", Importer.DEPLOYMENT_LOG);
        undeployed.markUndeployed(java.time.ZonedDateTime.now());
        System empty = System.builder().name("123").defaultOwner(blue).build();
        var source = ArchitectureModel.builder().teams(List.of(unused, blue)).systems(List.of(foo, empty)).build();
        when(repository.load()).thenReturn(source);
        String exported = export();
        assertThat(exported).contains("Team Blue", "Unused", "\"123\"", "contact-address", "jira-link")
                .doesNotContain("foo-service", "id:", "lastSeen", "null");
        assertThat(exported.indexOf("name: \"123\"")).isLessThan(exported.indexOf("name: \"foo\""));
        var properties = bind(exported);
        var emptyModel = ArchitectureModel.builder().build();
        new ModelOnboardingService(properties).apply(emptyModel);
        when(repository.load()).thenReturn(emptyModel);
        assertThat(export()).isEqualTo(exported);

        var teams = new ArrayList<>(properties.teams());
        var systems = new ArrayList<>(properties.systems());
        Collections.reverse(teams);
        Collections.reverse(systems);
        var reversed = ArchitectureModel.builder().build();
        new ModelOnboardingService(new ArchRepoModelProperties(teams, systems)).apply(reversed);
        when(repository.load()).thenReturn(reversed);
        assertThat(export()).isEqualTo(exported);
    }

    @Test
    void exportDoesNotRepairLegacyDuplicatesSoTheDocumentIsRejectedAtStartup() throws Exception {
        Team owner = Team.builder().name("team").build();
        when(repository.load()).thenReturn(ArchitectureModel.builder().teams(List.of(owner)).systems(List.of(
                System.builder().name("foo").defaultOwner(owner).aliases(List.of("shared")).build(),
                System.builder().name("FOO").defaultOwner(owner).aliases(List.of("SHARED")).build())).build());
        String exported = export();
        assertThat(exported).contains("foo", "FOO", "shared", "SHARED");
        assertThatThrownBy(() -> bind(exported))
                .rootCause().hasMessageContaining("foo").hasMessageContaining("FOO");
    }

    private String export() throws Exception {
        return mvc.perform(get("/api/model/export").accept("application/yaml"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/yaml"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static ArchRepoModelProperties bind(String yaml) throws Exception {
        var environment = new StandardEnvironment();
        var sources = new YamlPropertySourceLoader().load("export",
                new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));
        sources.forEach(environment.getPropertySources()::addFirst);
        return new Binder(ConfigurationPropertySources.get(environment))
                .bind("archrepo.model", Bindable.of(ArchRepoModelProperties.class))
                .orElseThrow(() -> new AssertionError("Export did not bind"));
    }
}
