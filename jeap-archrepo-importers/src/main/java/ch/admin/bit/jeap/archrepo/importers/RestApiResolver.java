package ch.admin.bit.jeap.archrepo.importers;

import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;

import java.util.List;
import java.util.Optional;

public interface RestApiResolver {

    /**
     * A REST API with exactly this path wins over one that only matches it loosely, e.g. one differing in a
     * trailing slash only. Otherwise an importer could pick the loose match while the exact one exists, and the
     * Grafana importer, which adopts the path it was given, would rename it into a duplicate of the exact one.
     */
    default Optional<RestApi> getRestApi(SystemComponent provider, String method, String path) {
        List<RestApi> restApis = provider.getParent().getRestApis().stream()
                .filter(restApi -> restApi.getProvider().equals(provider) && restApi.getMethod().equals(method))
                .toList();
        return restApis.stream()
                .filter(restApi -> restApi.getPath().equals(path))
                .findFirst()
                .or(() -> restApis.stream()
                        .filter(restApi -> restApi.pathMatches(path))
                        .findFirst())
                .or(() -> retrieveExistingRestApiWithMatchingPattern(provider, method, path));
    }

    Optional<RestApi> retrieveExistingRestApiWithMatchingPattern(SystemComponent provider, String method, String path);

}
