package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageVersionOrder;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The message type versions of the model, read one version at a time rather than through the whole architecture
 * model.
 * <p>
 * {@link MessageVersion} is an {@code @Embeddable} in an {@code @ElementCollection} and has no identity of its
 * own, so every query here reaches it through its {@link MessageType}. Loading the model instead would pull
 * every resolved schema of the landscape into memory to answer with one of them.
 */
@Repository
public interface MessageTypeVersionRepository extends JpaRepository<MessageType, UUID> {

    /**
     * The versions of every event, without their schemas.
     *
     * @param systemName restricts the result to one system, by its stored name and ignoring case. <b>Null means
     *                   the whole landscape</b> - an alias is resolved by the caller, because only the caller
     *                   can tell an unknown system from a system without messages
     */
    @Query("""
            select s.name as system, e.messageTypeName as message, 'EVENT' as kind, v.version as version
            from Event e
            join e.parent s
            join e.messageVersions v
            where :systemName is null or lower(s.name) = lower(:systemName)
            """)
    List<MessageTypeVersionIndexEntry> findEventIndexEntries(@Param("systemName") String systemName);

    /**
     * The versions of every command, without their schemas.
     *
     * @param systemName as in {@link #findEventIndexEntries(String)}
     */
    @Query("""
            select s.name as system, c.messageTypeName as message, 'COMMAND' as kind, v.version as version
            from Command c
            join c.parent s
            join c.messageVersions v
            where :systemName is null or lower(s.name) = lower(:systemName)
            """)
    List<MessageTypeVersionIndexEntry> findCommandIndexEntries(@Param("systemName") String systemName);

    /**
     * Every version of every message type, or of one system's message types, sorted by system, message type and
     * version.
     * <p>
     * Events and commands are separate entities and are read separately; the order is applied here so that the
     * index does not depend on which of the two queries ran first. Versions are ordered as numbers rather than
     * as text, or {@code 10.0.0} would stand between {@code 1.0.0} and {@code 2.0.0}.
     */
    default List<MessageTypeVersionIndexEntry> findIndexEntries(String systemName) {
        Comparator<MessageTypeVersionIndexEntry> order =
                Comparator.comparing(MessageTypeVersionIndexEntry::getSystem)
                        .thenComparing(MessageTypeVersionIndexEntry::getMessage)
                        .thenComparing(MessageTypeVersionIndexEntry::getVersion, MessageVersionOrder.INSTANCE);
        return Stream.concat(findEventIndexEntries(systemName).stream(),
                        findCommandIndexEntries(systemName).stream())
                .sorted(order)
                .toList();
    }

    /**
     * One version of one message type, with its schemas and with the message type name as it is stored.
     * <p>
     * A list rather than an {@code Optional}, because neither the schema nor the model forbids a system from
     * defining an event and a command of the same name: they would be two message types at one path, since a
     * version is addressed by system and name only. The registry's naming conventions make that combination
     * practically impossible, and answering with the first is a better failure than a {@code 500}.
     */
    @Query("""
            select new ch.admin.bit.jeap.archrepo.persistence.MessageTypeVersionDetail(
                mt.messageTypeName, v.version, v.compatibilityMode, v.compatibleVersion,
                v.keySchemaName, v.keySchemaUrl, v.keySchemaResolved,
                v.valueSchemaName, v.valueSchemaUrl, v.valueSchemaResolved)
            from MessageType mt
            join mt.parent s
            join mt.messageVersions v
            where lower(s.name) = lower(:systemName)
              and lower(mt.messageTypeName) = lower(:messageName)
              and v.version = :version
            """)
    List<MessageTypeVersionDetail> findVersions(@Param("systemName") String systemName,
                                                @Param("messageName") String messageName,
                                                @Param("version") String version);
}
