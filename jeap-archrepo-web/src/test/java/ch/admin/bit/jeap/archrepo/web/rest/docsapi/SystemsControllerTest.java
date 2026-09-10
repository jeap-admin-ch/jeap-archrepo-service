package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.persistence.ArchitectureModelRepository;
import ch.admin.bit.jeap.archrepo.web.config.WebSecurityConfig;
import ch.admin.bit.jeap.security.resource.configuration.MvcSecurityConfiguration;
import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import ch.admin.bit.jeap.security.resource.token.TokenConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SystemsController.class,
        properties = "archrepo.openapi-base-url=https://archrepo.example.com/archrepo-service/swagger-ui/index.html?url=/archrepo-service/api/openapi/")
@Import({DocsApiTestConfiguration.class, WebSecurityConfig.class,
        MvcSecurityConfiguration.class, ResourceServerProperties.class, TokenConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemsControllerTest extends DocsApiControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArchitectureModelRepository architectureModelRepository;

    @BeforeEach
    void setUp() {
        when(architectureModelRepository.load()).thenReturn(DocsApiModelStub.create());
    }

    @Test
    void getSystems() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS).accept(MediaType.APPLICATION_JSON)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(jsonPath("$.systems", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.systems[0].name").value(DocsApiModelStub.SYSTEM))
                .andExpect(jsonPath("$.systems[0].team.contactAddress").value("team-blue@example.com"));
    }

    @Test
    void getSystems_notModifiedWhenTheTagMatches() throws Exception {
        String etag = mockMvc.perform(get(DocsApiPaths.SYSTEMS)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("ETag");

        mockMvc.perform(get(DocsApiPaths.SYSTEMS).header("If-None-Match", etag)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""))
                // The 304 is written by Spring rather than by the ResponseEntity the 200 path builds, so the
                // cache directive has to be repeated onto it explicitly
                .andExpect(header().string("Cache-Control", "no-cache"));
    }

    @Test
    void getSystem() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/" + DocsApiModelStub.SYSTEM)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.name").value(DocsApiModelStub.SYSTEM))
                .andExpect(jsonPath("$.components", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.relations", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void getSystem_resolvesAnAlias() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/" + DocsApiModelStub.SYSTEM_ALIAS)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(DocsApiModelStub.SYSTEM));
    }

    @Test
    void getSystem_prefersTheNamedSystemOverAnotherSystemsAlias() throws Exception {
        // The systems come out of the database unordered, so the winner must not depend on which comes first
        for (boolean namedFirst : List.of(true, false)) {
            when(architectureModelRepository.load()).thenReturn(collidingAlias(namedFirst));

            mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/orders")
                            .with(authentication(tokenWithArchitectureModelRead())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("orders"));
        }
    }

    @Test
    void getMessages_prefersTheNamedSystemOverAnotherSystemsAlias() throws Exception {
        // The resource that served the aliased system's messages while the named one's stayed unread
        for (boolean namedFirst : List.of(true, false)) {
            when(architectureModelRepository.load()).thenReturn(collidingAlias(namedFirst));

            mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/orders/messages")
                            .with(authentication(tokenWithArchitectureModelRead())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages", org.hamcrest.Matchers.hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].name").value("OrdersOrderPlacedEvent"));
        }
    }

    @Test
    void getSystem_isCaseInsensitive() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/" + DocsApiModelStub.SYSTEM.toUpperCase())
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(DocsApiModelStub.SYSTEM));
    }

    @Test
    void getSystem_unknownSystemIsAProblemDocument() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/no-such-system")
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://jeap.admin.ch/problems/archrepo/system-not-found"))
                .andExpect(jsonPath("$.title").value("System not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No system or alias named 'no-such-system'"))
                .andExpect(jsonPath("$.instance").value("/docs-api/systems/no-such-system"));
    }

    @Test
    void getMessages() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/" + DocsApiModelStub.SYSTEM + "/messages")
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.messages", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void getMessages_unknownSystem() throws Exception {
        mockMvc.perform(get(DocsApiPaths.SYSTEMS + "/no-such-system/messages")
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound());
    }

    private static Event event(String name) {
        return Event.builder()
                .id(UUID.randomUUID())
                .messageTypeName(name)
                .scope("system")
                .topic("orders-events")
                .descriptorUrl("https://descriptors.example.com/" + name + ".json")
                .messageVersions(List.of())
                .build();
    }

    /** A landscape in which one system is named like another system's alias, each with a message of its own. */
    private static ArchitectureModel collidingAlias(boolean namedFirst) {
        System named = System.builder().name("orders").build();
        named.addEvent(event("OrdersOrderPlacedEvent"));
        System aliased = System.builder().name("billing").aliases(List.of("orders")).build();
        aliased.addEvent(event("BillingInvoiceSentEvent"));
        return ArchitectureModel.builder()
                .systems(namedFirst ? List.of(named, aliased) : List.of(aliased, named))
                .openApiBaseUrl(DocsApiModelStub.OPEN_API_BASE_URL)
                .build();
    }
}
