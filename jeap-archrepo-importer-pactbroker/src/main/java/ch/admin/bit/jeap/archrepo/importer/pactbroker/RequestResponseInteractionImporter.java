package ch.admin.bit.jeap.archrepo.importer.pactbroker;

import au.com.dius.pact.core.model.Request;
import au.com.dius.pact.core.model.RequestResponseInteraction;
import ch.admin.bit.jeap.archrepo.importers.RestApiResolver;
import ch.admin.bit.jeap.archrepo.importers.UrlHelper;
import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Slf4j
class RequestResponseInteractionImporter implements RestApiResolver {

    /**
     * Matches a path with or without a context prefix, providing a capturing group named "path" for the path without the context prefix.
     * The matching is done by assuming a single context path segment, followed by a segment named "api" or "something-api".
     *
     * <ul>
     *     <li>/context/api/resource -> /api/resource</li>
     *     <li>/context/ui-api/resource -> /ui-api/resource</li>
     *     <li>/api/resource -> /api/resource</li>
     * </ul>
     */
    private static final Pattern PATH_WITH_CONTEXT_PREFIX_PATTERN =
            Pattern.compile("(?<context>/.*?)(?<path>/(?<segregatedapi>.+?-)?api/.*)");

    private final SystemComponent consumer;
    private final SystemComponent provider;
    private final String pactUrl;

    public void importInteraction(RequestResponseInteraction interaction) {

        Request request = interaction.getRequest();
        String pathWithoutContextPrefix = pathWithoutContextPrefix(request.getPath());
        Optional<RestApi> optionalRestApi = retrieveExistingRestApi(request.getMethod(), pathWithoutContextPrefix);

        if (optionalRestApi.isEmpty()) {
            // try to match with the context prefix
            optionalRestApi = retrieveExistingRestApi(request.getMethod(), request.getPath());
        }

        if (optionalRestApi.isEmpty()) {
            RestApi restApi = RestApi.builder()
                    .provider(provider)
                    .method(request.getMethod())
                    .path(pathWithoutContextPrefix)
                    .importer(Importer.PACT_BROKER)
                    .build();
            provider.getParent().addRestApi(restApi);
            log.info("Create new rest api {}", restApi);
            optionalRestApi = Optional.of(restApi);
        }

        RestApiRelation relation = RestApiRelation.builder()
                .consumerName(consumer.getName())
                .providerName(provider.getName())
                .pactUrl(pactUrl)
                .restApi(optionalRestApi.get())
                .importer(Importer.PACT_BROKER)
                .lastSeen(ZonedDateTime.now())
                .build();

        Optional<RestApiRelation> relationOptional = retrieveExistingRelation(relation);

        if (relationOptional.isEmpty()) {
            provider.getParent().addRelation(relation);
            log.info("Added relation for pact {}", pactUrl);
        } else {
            RestApiRelation existingRelation = relationOptional.get();
            if (!existingRelation.getImporters().contains(Importer.PACT_BROKER) || !pactUrl.equals(existingRelation.getPactUrl())) {
                existingRelation.addImporter(Importer.PACT_BROKER);
                existingRelation.setPactUrl(pactUrl);
                existingRelation.setLastSeen(ZonedDateTime.now());
                log.info("Updating existing relation with path '{}' with pact with path '{}'",
                        existingRelation.getRestApi().getPath(),
                        relation.getRestApi().getPath());
                log.info("Updated pactUrl '{}' to existing relation '{}' and updated last-seen", pactUrl, existingRelation);
            }
        }
    }

    private String pathWithoutContextPrefix(String path) {
        Matcher matcher = PATH_WITH_CONTEXT_PREFIX_PATTERN.matcher(path);
        if (matcher.matches()) {
            return matcher.group("path");
        } else {
            return path;
        }
    }

    private Optional<RestApiRelation> retrieveExistingRelation(RestApiRelation relation) {
        return provider.getParent().getRelations().stream()
                .filter(RestApiRelation.class::isInstance)
                .map(RestApiRelation.class::cast)
                .filter(r -> r.getConsumerName().equals(relation.getConsumerName()) &&
                             r.getProviderName().equals(relation.getProviderName()) &&
                             r.getRestApi().getId().equals(relation.getRestApi().getId())
                )
                .findFirst();
    }

    private Optional<RestApi> retrieveExistingRestApi(String httpMethod, String path) {
        Optional<RestApi> optionalRestApi = this.getRestApi(provider, httpMethod, path);

        if (optionalRestApi.isPresent()) {
            if (!optionalRestApi.get().getImporters().contains(Importer.PACT_BROKER)) {
                optionalRestApi.get().addImporter(Importer.PACT_BROKER);
            }
            log.info("Found available rest api {}", optionalRestApi.get());
            return optionalRestApi;
        }

        return Optional.empty();
    }

    @Override
    public Optional<RestApi> retrieveExistingRestApiWithMatchingPattern(SystemComponent provider, String method, String path) {
        List<RestApi> matchingRestApis = new ArrayList<>();
        for (RestApi currentRestApi : provider.getParent().getRestApis()) {
            if (UrlHelper.restApisProviderMethodAreMatching(currentRestApi, provider, method)) {
                // Match path to provider APIs
                final String currentRestApiPathWithoutContextPrefix = pathWithoutContextPrefix(currentRestApi.getPath());
                final String pathRegex = UrlHelper.convertPathToRegex(currentRestApiPathWithoutContextPrefix);
                Matcher matcher = Pattern.compile(pathRegex, Pattern.CASE_INSENSITIVE)
                        .matcher(UrlHelper.removeTrailingSlash(path));
                if (matcher.matches()) {
                    log.debug("Found matching rest api with regex from jeap ('{}') with the pact relation '{}'",
                            currentRestApi.getPath(),
                            path);
                    matchingRestApis.add(currentRestApi);
                } else {

                    // Match provided REST API to path
                    matcher = Pattern.compile(UrlHelper.convertPathToRegex(path), Pattern.CASE_INSENSITIVE)
                            .matcher(UrlHelper.removeTrailingSlash(currentRestApiPathWithoutContextPrefix));
                    if (matcher.matches()) {
                        log.debug("Found matching rest api with regex from jeap ('{}') with the pact relation '{}'",
                                currentRestApi.getPath(),
                                path);
                        matchingRestApis.add(currentRestApi);
                    }
                }

            }
        }

        if (matchingRestApis.isEmpty()) {
            log.debug("No rest api present for provider {} on {} : {}", provider, method, path);
            return Optional.empty();
        } else if (matchingRestApis.size() == 1) {
            return Optional.of(matchingRestApis.getFirst());
        } else {
            log.info("Multiple matching rest apis found for provider '{}' on '{}' '{}'. Matching rest apis: {}",
                    provider.getName(), method, path, matchingRestApis);

            // Find the most specific matching rest api (the one with the less path variables)
            matchingRestApis.sort(Comparator.comparingInt(api -> UrlHelper.countPathVariables(api.getPath())));

            return Optional.of(matchingRestApis.getFirst());
        }
    }
}
