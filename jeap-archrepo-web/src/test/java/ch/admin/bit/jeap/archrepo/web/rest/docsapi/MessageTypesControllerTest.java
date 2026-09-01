package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.persistence.MessageTypeVersionDetail;
import ch.admin.bit.jeap.archrepo.persistence.MessageTypeVersionIndexEntry;
import ch.admin.bit.jeap.archrepo.persistence.MessageTypeVersionRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
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
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MessageTypesController.class)
@Import({DocsApiTestConfiguration.class, WebSecurityConfig.class,
        MvcSecurityConfiguration.class, ResourceServerProperties.class, TokenConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MessageTypesControllerTest extends DocsApiControllerTestBase {

    private static final String SYSTEM = "wvs";
    private static final String ALIAS = "WVS-ALIAS";
    private static final String EVENT = "WvsDeclarationAcceptedEvent";
    private static final String COMMAND = "WvsCheckNctsReferabilityV2Command";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageTypeVersionRepository messageTypeVersionRepository;
    @MockitoBean
    private SystemRepository systemRepository;

    @BeforeEach
    void setUp() {
        when(messageTypeVersionRepository.findIndexEntries(null)).thenReturn(List.of(
                indexEntry(SYSTEM, EVENT, "EVENT", "1.0.0"),
                indexEntry(SYSTEM, EVENT, "EVENT", "2.0.0"),
                indexEntry(SYSTEM, COMMAND, "COMMAND", "1.0.0")));
        when(messageTypeVersionRepository.findIndexEntries(SYSTEM))
                .thenReturn(List.of(indexEntry(SYSTEM, EVENT, "EVENT", "1.0.0")));
        when(messageTypeVersionRepository.findVersions(SYSTEM, EVENT, "2.0.0"))
                .thenReturn(List.of(detail()));
        when(messageTypeVersionRepository.findVersions(SYSTEM, EVENT, "9.9.9")).thenReturn(List.of());

        when(systemRepository.findByNameOrAliasIgnoreCase(SYSTEM))
                .thenReturn(Optional.of(System.builder().name(SYSTEM).build()));
        when(systemRepository.findByNameOrAliasIgnoreCase(ALIAS))
                .thenReturn(Optional.of(System.builder().name(SYSTEM).build()));
        when(systemRepository.findByNameOrAliasIgnoreCase("no-such-system")).thenReturn(Optional.empty());
    }

    @Test
    void getMessageTypes() throws Exception {
        mockMvc.perform(get(DocsApiPaths.MESSAGE_TYPES).accept(MediaType.APPLICATION_JSON)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Cache-Control", "no-cache"))
                // The three rows are two message types: the versions of one are grouped under it
                .andExpect(jsonPath("$.messageTypes", hasSize(2)))
                .andExpect(jsonPath("$.messageTypes[0].system").value(SYSTEM))
                .andExpect(jsonPath("$.messageTypes[0].message").value(EVENT))
                .andExpect(jsonPath("$.messageTypes[0].kind").value("EVENT"))
                .andExpect(jsonPath("$.messageTypes[0].versions", hasSize(2)))
                .andExpect(jsonPath("$.messageTypes[0].versions[0].version").value("1.0.0"))
                .andExpect(jsonPath("$.messageTypes[0].versions[0].contentUrl")
                        .value("/docs-api/message-types/wvs/" + EVENT + "/versions/1.0.0"))
                .andExpect(jsonPath("$.messageTypes[1].message").value(COMMAND))
                .andExpect(jsonPath("$.messageTypes[1].kind").value("COMMAND"));
    }

    @Test
    void getMessageTypes_carriesNoSchema() throws Exception {
        // The index exists so that deciding what to fetch never reads a schema; a schema in it would defeat that
        mockMvc.perform(get(DocsApiPaths.MESSAGE_TYPES)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageTypes[0].versions[0].resolvedSchema").doesNotExist());
    }

    @Test
    void getMessageTypes_notModifiedWhenTheTagMatches() throws Exception {
        String etag = mockMvc.perform(get(DocsApiPaths.MESSAGE_TYPES)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("ETag");

        mockMvc.perform(get(DocsApiPaths.MESSAGE_TYPES).header("If-None-Match", etag)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""))
                .andExpect(header().string("Cache-Control", "no-cache"));
    }

    @Test
    void getMessageTypes_filteredBySystem() throws Exception {
        mockMvc.perform(get(DocsApiPaths.MESSAGE_TYPES).param("system", SYSTEM)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageTypes", hasSize(1)));
    }

    @Test
    void getMessageTypes_theFilterResolvesAnAlias() throws Exception {
        mockMvc.perform(get(DocsApiPaths.MESSAGE_TYPES).param("system", ALIAS)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageTypes", hasSize(1)));
    }

    @Test
    void getMessageTypes_unknownSystemFilterIsAProblemDocument() throws Exception {
        mockMvc.perform(get(DocsApiPaths.MESSAGE_TYPES).param("system", "no-such-system")
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://jeap.admin.ch/problems/archrepo/system-not-found"));
    }

    @Test
    void getMessageTypeVersion() throws Exception {
        mockMvc.perform(get(DocsApiPaths.messageTypeVersionPath(SYSTEM, EVENT, "2.0.0"))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andExpect(jsonPath("$.system").value(SYSTEM))
                .andExpect(jsonPath("$.message").value(EVENT))
                .andExpect(jsonPath("$.version").value("2.0.0"))
                .andExpect(jsonPath("$.compatibilityMode").value("BACKWARD"))
                .andExpect(jsonPath("$.compatibleVersion").value("1.0.0"))
                .andExpect(jsonPath("$.key.schemaName").value("Key.avdl"))
                .andExpect(jsonPath("$.key.resolvedSchema").value("key schema"))
                .andExpect(jsonPath("$.value.schemaName").value("Value.avdl"))
                .andExpect(jsonPath("$.value.schemaUrl").value("https://registry.example.com/Value.avdl"))
                .andExpect(jsonPath("$.value.resolvedSchema").value("value schema"));
    }

    @Test
    void getMessageTypeVersion_notModifiedWhenTheTagMatches() throws Exception {
        String path = DocsApiPaths.messageTypeVersionPath(SYSTEM, EVENT, "2.0.0");
        String etag = mockMvc.perform(get(path).with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("ETag");

        mockMvc.perform(get(path).header("If-None-Match", etag)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""))
                .andExpect(header().string("Cache-Control", "no-cache"));
    }

    @Test
    void getMessageTypeVersion_namesTheMessageTypeAsItIsStored() throws Exception {
        // The path matches ignoring case; echoing the caller's spelling would give one resource two bodies
        when(messageTypeVersionRepository.findVersions(SYSTEM, EVENT.toLowerCase(), "2.0.0"))
                .thenReturn(List.of(detail()));

        mockMvc.perform(get(DocsApiPaths.messageTypeVersionPath(SYSTEM, EVENT.toLowerCase(), "2.0.0"))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(EVENT));
    }

    @Test
    void getMessageTypes_anEventAndACommandOfOneNameStayApart() throws Exception {
        // Nothing forbids the two, and merging them would hide one message type behind the other's kind
        when(messageTypeVersionRepository.findIndexEntries(null)).thenReturn(List.of(
                indexEntry(SYSTEM, "WvsThing", "COMMAND", "1.0.0"),
                indexEntry(SYSTEM, "WvsThing", "EVENT", "3.0.0")));

        mockMvc.perform(get(DocsApiPaths.MESSAGE_TYPES)
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageTypes", hasSize(2)))
                .andExpect(jsonPath("$.messageTypes[0].kind").value("COMMAND"))
                .andExpect(jsonPath("$.messageTypes[0].versions", hasSize(1)))
                .andExpect(jsonPath("$.messageTypes[1].kind").value("EVENT"))
                .andExpect(jsonPath("$.messageTypes[1].versions[0].version").value("3.0.0"));
    }

    @Test
    void getMessageTypeVersion_withoutAKeySchemaTheKeyIsAbsent() throws Exception {
        when(messageTypeVersionRepository.findVersions(SYSTEM, EVENT, "1.0.0"))
                .thenReturn(List.of(new MessageTypeVersionDetail(EVENT, "1.0.0", null, null,
                        null, null, null,
                        "Value.avdl", "https://registry.example.com/Value.avdl", "value schema")));

        mockMvc.perform(get(DocsApiPaths.messageTypeVersionPath(SYSTEM, EVENT, "1.0.0"))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                // Absent rather than an object of three nulls: the two say different things
                .andExpect(jsonPath("$.key").doesNotExist())
                .andExpect(jsonPath("$.value.schemaName").value("Value.avdl"));
    }

    @Test
    void getMessageTypeVersion_resolvesAnAlias() throws Exception {
        mockMvc.perform(get(DocsApiPaths.messageTypeVersionPath(ALIAS, EVENT, "2.0.0"))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.system").value(SYSTEM));
    }

    @Test
    void getMessageTypeVersion_unknownSystemIsAProblemDocument() throws Exception {
        mockMvc.perform(get(DocsApiPaths.messageTypeVersionPath("no-such-system", EVENT, "2.0.0"))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://jeap.admin.ch/problems/archrepo/system-not-found"));
    }

    @Test
    void getMessageTypeVersion_unknownVersionIsAProblemDocument() throws Exception {
        mockMvc.perform(get(DocsApiPaths.messageTypeVersionPath(SYSTEM, EVENT, "9.9.9"))
                        .with(authentication(tokenWithArchitectureModelRead())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("https://jeap.admin.ch/problems/archrepo/message-type-version-not-found"))
                .andExpect(jsonPath("$.title").value("Message type version not found"))
                .andExpect(jsonPath("$.detail")
                        .value("System 'wvs' has no message type '" + EVENT + "' in version '9.9.9'"));
    }

    private static MessageTypeVersionDetail detail() {
        return new MessageTypeVersionDetail(EVENT, "2.0.0", "BACKWARD", "1.0.0",
                "Key.avdl", "https://registry.example.com/Key.avdl", "key schema",
                "Value.avdl", "https://registry.example.com/Value.avdl", "value schema");
    }

    private static MessageTypeVersionIndexEntry indexEntry(String system, String message, String kind,
                                                           String version) {
        return new MessageTypeVersionIndexEntry() {
            @Override
            public String getSystem() {
                return system;
            }

            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public String getKind() {
                return kind;
            }

            @Override
            public String getVersion() {
                return version;
            }
        };
    }
}
