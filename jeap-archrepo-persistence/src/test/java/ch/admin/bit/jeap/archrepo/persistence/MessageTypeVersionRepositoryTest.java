package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest(properties = "spring.flyway.locations=classpath:db/migration/common")
class MessageTypeVersionRepositoryTest extends PostgresDataJpaTestBase {

    @Autowired
    private MessageTypeVersionRepository messageTypeVersionRepository;

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void findIndexEntries_returnsEventAndCommandVersionsSortedBySystemMessageAndVersion() {
        System alpha = createSystem("alpha");
        alpha.addEvent(event("ShipmentEvent", List.of(
                messageVersion("10.0.0"),
                messageVersion("1.0.0"),
                messageVersion("2.0.0"))));
        alpha.addCommand(command("InvoiceCommand", List.of(messageVersion("1.0.0"))));
        systemRepository.save(alpha);

        System beta = createSystem("beta");
        beta.addEvent(event("AccountEvent", List.of(messageVersion("1.0.0"))));
        systemRepository.saveAndFlush(beta);

        List<MessageTypeVersionIndexEntry> entries = messageTypeVersionRepository.findIndexEntries(null);

        assertThat(entries)
                .extracting(MessageTypeVersionIndexEntry::getSystem,
                        MessageTypeVersionIndexEntry::getMessage,
                        MessageTypeVersionIndexEntry::getKind,
                        MessageTypeVersionIndexEntry::getVersion)
                .containsExactly(
                        tuple("alpha", "InvoiceCommand", "COMMAND", "1.0.0"),
                        tuple("alpha", "ShipmentEvent", "EVENT", "1.0.0"),
                        tuple("alpha", "ShipmentEvent", "EVENT", "2.0.0"),
                        tuple("alpha", "ShipmentEvent", "EVENT", "10.0.0"),
                        tuple("beta", "AccountEvent", "EVENT", "1.0.0"));
    }

    @Test
    void findIndexEntries_filtersBySystemNameIgnoringCase() {
        System alpha = createSystem("alpha");
        alpha.addEvent(event("ShipmentEvent", List.of(messageVersion("1.0.0"))));
        systemRepository.save(alpha);

        System beta = createSystem("beta");
        beta.addCommand(command("InvoiceCommand", List.of(messageVersion("1.0.0"))));
        systemRepository.saveAndFlush(beta);

        assertThat(messageTypeVersionRepository.findIndexEntries("ALPHA"))
                .extracting(MessageTypeVersionIndexEntry::getSystem,
                        MessageTypeVersionIndexEntry::getMessage,
                        MessageTypeVersionIndexEntry::getKind,
                        MessageTypeVersionIndexEntry::getVersion)
                .containsExactly(tuple("alpha", "ShipmentEvent", "EVENT", "1.0.0"));
        assertThat(messageTypeVersionRepository.findIndexEntries("no-such-system")).isEmpty();
    }

    @Test
    void findVersions_returnsStoredMessageNameAndSchemaDetailsIgnoringCase() {
        System system = createSystem("wvs");
        system.addEvent(event("WvsDeclarationAcceptedEvent", List.of(
                messageVersion("1.0.0"),
                MessageVersion.builder()
                        .version("2.0.0")
                        .keySchemaName("Key.avdl")
                        .keySchemaUrl("https://registry.example.com/Key.avdl")
                        .keySchemaResolved("key schema")
                        .valueSchemaName("Value.avdl")
                        .valueSchemaUrl("https://registry.example.com/Value.avdl")
                        .valueSchemaResolved("value schema")
                        .compatibleVersion("1.0.0")
                        .compatibilityMode("BACKWARD")
                        .build())));
        systemRepository.saveAndFlush(system);

        List<MessageTypeVersionDetail> versions = messageTypeVersionRepository.findVersions(
                "WVS", "wvsdeclarationacceptedevent", "2.0.0");

        assertThat(versions)
                .containsExactly(new MessageTypeVersionDetail(
                        "WvsDeclarationAcceptedEvent", "2.0.0", "BACKWARD", "1.0.0",
                        "Key.avdl", "https://registry.example.com/Key.avdl", "key schema",
                        "Value.avdl", "https://registry.example.com/Value.avdl", "value schema"));
    }

    @Test
    void findVersions_isEmptyForUnknownVersion() {
        System system = createSystem("wvs");
        system.addEvent(event("WvsDeclarationAcceptedEvent", List.of(messageVersion("1.0.0"))));
        systemRepository.saveAndFlush(system);

        assertThat(messageTypeVersionRepository.findVersions(
                "wvs", "WvsDeclarationAcceptedEvent", "2.0.0")).isEmpty();
    }

    private System createSystem(String name) {
        Team team = teamRepository.save(Team.builder().name(name + "-team").build());
        return System.builder().name(name).defaultOwner(team).build();
    }

    private Event event(String name, List<MessageVersion> versions) {
        return Event.builder()
                .id(UUID.randomUUID())
                .messageTypeName(name)
                .descriptorUrl("https://registry.example.com/" + name + ".avdl")
                .messageVersions(versions)
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .scope("public")
                .build();
    }

    private Command command(String name, List<MessageVersion> versions) {
        return Command.builder()
                .id(UUID.randomUUID())
                .messageTypeName(name)
                .descriptorUrl("https://registry.example.com/" + name + ".avdl")
                .messageVersions(versions)
                .importer(Importer.MESSAGE_TYPE_REGISTRY)
                .scope("public")
                .build();
    }

    private MessageVersion messageVersion(String version) {
        return MessageVersion.builder()
                .version(version)
                .valueSchemaName("Value.avdl")
                .valueSchemaUrl("https://registry.example.com/Value.avdl")
                .valueSchemaResolved("value schema " + version)
                .build();
    }
}
