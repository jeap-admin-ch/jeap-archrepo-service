package ch.admin.bit.jeap.archrepo.metamodel.message;

import jakarta.persistence.Embeddable;
import lombok.*;

@NoArgsConstructor
@Builder
@AllArgsConstructor
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

    public void setKeySchemaName(String keySchemaName) {
        if (keySchemaName == null || keySchemaName.isEmpty()) {
            return;
        }
        this.keySchemaName = keySchemaName;
    }

    public void setKeySchemaUrl(String keySchemaUrl) {
        if (keySchemaUrl == null || keySchemaUrl.isEmpty()) {
            return;
        }
        this.keySchemaUrl = keySchemaUrl;
    }
}
