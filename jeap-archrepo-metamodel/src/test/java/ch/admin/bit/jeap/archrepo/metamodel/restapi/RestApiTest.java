package ch.admin.bit.jeap.archrepo.metamodel.restapi;

import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestApiTest {

    @Test
    void pathMatches() {
        RestApi resource = RestApi.builder()
                .provider(BackendService.builder().name("provider").build())
                .path("/foo/{var1}/{var2}/bar")
                .method("GET")
                .build();

        assertTrue(resource.pathMatches("/foo/{}/{}/bar"));
        assertTrue(resource.pathMatches("/foo/{a}/{b}/bar"));
        assertTrue(resource.pathMatches("/foo/{param}/{param}/bar"));
        assertFalse(resource.pathMatches("/foo/path/path/bar"));
        assertFalse(resource.pathMatches("/foo/bar"));
    }
}
