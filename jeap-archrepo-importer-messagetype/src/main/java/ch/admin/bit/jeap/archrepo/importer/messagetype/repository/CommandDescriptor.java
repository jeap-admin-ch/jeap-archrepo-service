package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class CommandDescriptor implements MessageTypeDescriptor {
    public static final String SUBDIR = "command";

    @NonNull String definingSystem;
    @JsonAlias("commandName")
    @NonNull String messageTypeName;
    @NonNull String description;
    String documentationUrl;
    String topic;
    @NonNull String scope;
    CommandContracts contracts;
    List<MessageTypeVersion> versions;

    public CommandContracts getContracts() {
        return contracts == null ? CommandContracts.NONE : contracts;
    }

    public List<MessageTypeVersion> getVersions() {
        return versions == null ? List.of() : versions;
    }

    @Override
    public String getMessageTypeSubdir() {
        return SUBDIR;
    }
}
