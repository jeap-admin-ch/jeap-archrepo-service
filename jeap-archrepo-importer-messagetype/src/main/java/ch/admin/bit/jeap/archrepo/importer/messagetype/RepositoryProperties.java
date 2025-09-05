package ch.admin.bit.jeap.archrepo.importer.messagetype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

@AllArgsConstructor
@Data
public class RepositoryProperties {

    private final String uri;
    private final RepositoryType type;
    @ToString.Exclude
    private Map<String, String> parameters = Map.of();

    public RepositoryProperties(String uri, RepositoryType type) {
        this.uri = uri;
        this.type = type;
    }

    public enum RepositoryType {
        BITBUCKET,
        GITHUB,
        LOCAL
    }

}

