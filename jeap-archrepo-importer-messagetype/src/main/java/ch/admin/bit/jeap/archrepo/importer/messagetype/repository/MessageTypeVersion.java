package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import lombok.NonNull;
import lombok.Value;

@Value
public class MessageTypeVersion {
    @NonNull
    String version;
    String keySchema;
    @NonNull String valueSchema;
    String compatibilityMode;
    String compatibleVersion;

    public SemanticVersion semanticVersion() {
        return SemanticVersion.parse(version);
    }
}
