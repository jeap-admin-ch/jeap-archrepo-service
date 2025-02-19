package ch.admin.bit.jeap.archrepo.importer.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.JeapRelation;
import ch.admin.bit.jeap.archrepo.importers.RestApiResolver;
import ch.admin.bit.jeap.archrepo.importers.UrlHelper;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Slf4j
public class JeapRelationImporter implements RestApiResolver {

    private final SystemComponent consumer;
    private final SystemComponent provider;

    public void importRelation(JeapRelation jeapRelation) {
        RestApiRelation relation = RestApiRelation.builder()
                .consumerName(consumer.getName())
                .providerName(provider.getName())
                .restApi(getOrCreateRestApi(jeapRelation.getMethod(), jeapRelation.getDatapoint()))
                .importer(Importer.GRAFANA)
                .lastSeen(ZonedDateTime.now())
                .build();

        Optional<RestApiRelation> relationOptional = provider.getParent().getRelations().stream()
                .filter(RestApiRelation.class::isInstance)
                .map(RestApiRelation.class::cast)
                .filter(r -> r.getConsumerName().equals(relation.getConsumerName()) &&
                        r.getProviderName().equals(relation.getProviderName()) &&
                        r.getRestApi().getId().equals(relation.getRestApi().getId())
                )
                .findFirst();

        if (relationOptional.isEmpty()) {
            provider.getParent().addRelation(relation);
            log.info("Added to parent '{}' new relation '{}'.", provider.getParent().getName(), jeapRelation);
        } else {
            RestApiRelation existingRelation = relationOptional.get();
            if (!existingRelation.getImporters().contains(Importer.GRAFANA)) {
                existingRelation.addImporter(Importer.GRAFANA);
                log.info("Added Importer.GRAFANA to existing relation '{}'.", existingRelation);
            }
            existingRelation.setLastSeen(ZonedDateTime.now());
            log.info("Updated last-seen to existing relation '{}'.", existingRelation);
        }
    }

    private RestApi getOrCreateRestApi(String httpMethod, String path){
        Optional<RestApi> optionalRestApi = this.getRestApi(provider, httpMethod, path);

        RestApi restApi;
        if(optionalRestApi.isPresent()) {
            restApi = optionalRestApi.get();
            if (!restApi.getImporters().contains(Importer.GRAFANA)){
                restApi.updatePath(path);
                restApi.addImporter(Importer.GRAFANA);
            }
            log.info("Retrieved available rest api {}", restApi);
        } else {
            restApi = RestApi.builder()
                    .provider(provider)
                    .method(httpMethod)
                    .path(path)
                    .importer(Importer.GRAFANA)
                    .build();
            provider.getParent().addRestApi(restApi);
            log.info("Create new rest api {}", restApi);
        }
        return restApi;
    }

    @Override
    public Optional<RestApi> retrieveExistingRestApiWithMatchingPattern(SystemComponent provider, String method, String path) {
        for (RestApi currentRestApi : provider.getParent().getRestApis()) {
            if (UrlHelper.restApisProviderMethodAreMatching(currentRestApi, provider, method)) {
                // Match path to provider APIs
                final String pathRegex = UrlHelper.convertPathToRegex(currentRestApi.getPath());
                Matcher matcher = Pattern.compile(pathRegex, Pattern.CASE_INSENSITIVE)
                        .matcher(UrlHelper.removeTrailingSlash(path));
                if (matcher.matches()) {
                    log.debug("Found matching rest api with regex from jeap ('{}') with the pact relation '{}'",
                            currentRestApi.getPath(),
                            path);
                    return Optional.of(currentRestApi);
                }

                // Match provided REST API to path
                matcher = Pattern.compile(UrlHelper.convertPathToRegex(path), Pattern.CASE_INSENSITIVE)
                        .matcher(UrlHelper.removeTrailingSlash(currentRestApi.getPath()));
                if (matcher.matches()) {
                    // Do not match generic endpoints to specific ones
                    if (!UrlHelper.hasPathVariable(path) || UrlHelper.hasPathVariable(currentRestApi.getPath())) {
                        log.debug("Found matching rest api with regex from jeap ('{}') with the pact relation '{}'",
                                currentRestApi.getPath(),
                                path);
                        return Optional.of(currentRestApi);
                    }
                }

            }
        }

        log.debug("No rest api present for provider {} on {} : {}", provider, method, path);
        return Optional.empty();
    }
}
