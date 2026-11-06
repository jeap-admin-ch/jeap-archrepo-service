package ch.admin.bit.jeap.archrepo.metamodel.message;

import ch.admin.bit.jeap.archrepo.metamodel.ContentHash;
import jakarta.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor
@Getter
@Embeddable
public class MessageVersion {
    private String version;
    private String keySchemaName;
    private String keySchemaUrl;
    private String keySchemaResolved;
    @NonNull
    private String valueSchemaName;
    @NonNull
    private String valueSchemaUrl;
    @NonNull
    private String valueSchemaResolved;
    private String compatibleVersion;
    private String compatibilityMode;

    /**
     * SHA-256 over everything the docs API serves about this version, so that the message type index can carry
     * the version's entity tag without reading its schemas - see {@link #computeContentHash()}.
     * <p>
     * Computed here, whenever the version is built or changed, and never set from outside: a hash that could be
     * set independently of the fields could disagree with them. Null on a row stored before the column existed,
     * until the next model update writes the version again.
     */
    private String contentHash;

    @Builder
    public MessageVersion(String version,
                          String keySchemaName,
                          String keySchemaUrl,
                          String keySchemaResolved,
                          @NonNull String valueSchemaName,
                          @NonNull String valueSchemaUrl,
                          @NonNull String valueSchemaResolved,
                          String compatibleVersion,
                          String compatibilityMode) {
        this.version = version;
        this.keySchemaName = keySchemaName;
        this.keySchemaUrl = keySchemaUrl;
        this.keySchemaResolved = keySchemaResolved;
        this.valueSchemaName = valueSchemaName;
        this.valueSchemaUrl = valueSchemaUrl;
        this.valueSchemaResolved = valueSchemaResolved;
        this.compatibleVersion = compatibleVersion;
        this.compatibilityMode = compatibilityMode;
        this.contentHash = computeContentHash();
    }

    public void setKeySchemaName(String keySchemaName) {
        if (keySchemaName == null || keySchemaName.isEmpty()) {
            return;
        }
        this.keySchemaName = keySchemaName;
        this.contentHash = computeContentHash();
    }

    public void setKeySchemaUrl(String keySchemaUrl) {
        if (keySchemaUrl == null || keySchemaUrl.isEmpty()) {
            return;
        }
        this.keySchemaUrl = keySchemaUrl;
        this.contentHash = computeContentHash();
    }

    /**
     * The hash over exactly the fields the docs API serves for a version. The compatible version is one of them:
     * it is derived from the version list when the message type is imported, so an intermediate version published
     * later changes this hash too. The system and the message type are not stored here and are folded in where
     * the entity tag is built.
     */
    private String computeContentHash() {
        return ContentHash.ofValues(version, compatibilityMode, compatibleVersion,
                keySchemaName, keySchemaUrl, keySchemaResolved,
                valueSchemaName, valueSchemaUrl, valueSchemaResolved);
    }
}
