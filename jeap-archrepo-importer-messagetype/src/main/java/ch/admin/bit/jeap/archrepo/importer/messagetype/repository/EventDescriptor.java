package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class EventDescriptor implements MessageTypeDescriptor {
    public static final String SUBDIR = "event";

    @JsonAlias("publishingSystem")
    @NonNull String definingSystem;
    @JsonAlias("eventName")
    @NonNull String messageTypeName;
    @NonNull String description;
    String documentationUrl;
    String topic;
    @NonNull String scope;
    EventContracts contracts;
    List<MessageTypeVersion> versions;

    public EventContracts getContracts() {
        return contracts == null ? EventContracts.NONE : contracts;
    }

    public List<MessageTypeVersion> getVersions() {
        return versions == null ? List.of() : versions;
    }

    @Override
    public String getMessageTypeSubdir() {
        return SUBDIR;
    }
}
