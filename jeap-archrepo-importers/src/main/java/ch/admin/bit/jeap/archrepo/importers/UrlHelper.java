package ch.admin.bit.jeap.archrepo.importers;

import ch.admin.bit.jeap.archrepo.metamodel.restapi.RestApi;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UrlHelper {

    public static final String REGEX_RETRIEVE_PARAMS = "(\\{[^}]*})";
    public static final String REGEX_MATCHING_PARAM = "([A-Z0-9-_.]+)";

    public static boolean restApisProviderMethodAreMatching(RestApi restApi, SystemComponent provider, String method) {
        return restApi.getProvider().equals(provider) && restApi.getMethod().equals(method);
    }

    public static String convertPathToRegex(String path) {
        return "(/[A-Z0-9-_]+)?" + path.replaceAll(REGEX_RETRIEVE_PARAMS, REGEX_MATCHING_PARAM);
    }

    public static String removeTrailingSlash(String input) {
        if (input.endsWith("/")) {
            return input.substring(0, input.length() - 1);
        }
        return input;
    }

    public static boolean hasPathVariable(String path) {
        return path.matches(".*\\{.*}.*");
    }
}
