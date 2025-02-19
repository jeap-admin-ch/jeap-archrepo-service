package ch.admin.bit.jeap.archrepo.importers;

import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;

import java.util.List;
import java.util.Optional;

public interface RestApiResolver {

    default Optional<RestApi> getRestApi(SystemComponent provider, String method, String path) {
        List<RestApi> restApis = provider.getParent().getRestApis();
        Optional<RestApi> optionalRestApi = restApis.stream()
                .filter(restApi -> restApi.getProvider().equals(provider) && restApi.getMethod().equals(method))
                .filter(restApi -> restApi.pathMatches(path))
                .findFirst();
        if (optionalRestApi.isPresent()) {
            return optionalRestApi;
        } else {
            return retrieveExistingRestApiWithMatchingPattern(provider, method, path);
        }
    }

    Optional<RestApi> retrieveExistingRestApiWithMatchingPattern(SystemComponent provider, String method, String path);

}
