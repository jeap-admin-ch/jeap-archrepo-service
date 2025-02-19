package ch.admin.bit.jeap.archrepo.importers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UrlHelperTest {

    @ParameterizedTest
    @CsvSource({
            "/api/contexts/{param}/documents/{param}/test, (/[A-Z0-9-_]+)?/api/contexts/([A-Z0-9-_.]+)/documents/([A-Z0-9-_.]+)/test",
            "/api/{param}/, (/[A-Z0-9-_]+)?/api/([A-Z0-9-_.]+)/",
            "/{param}, (/[A-Z0-9-_]+)?/([A-Z0-9-_.]+)",
            "/{param}/{id}/{test},(/[A-Z0-9-_]+)?/([A-Z0-9-_.]+)/([A-Z0-9-_.]+)/([A-Z0-9-_.]+)",
            "/api/{param}, (/[A-Z0-9-_]+)?/api/([A-Z0-9-_.]+)",
            "/foo/{docId}/bar/{uuid}/foo, (/[A-Z0-9-_]+)?/foo/([A-Z0-9-_.]+)/bar/([A-Z0-9-_.]+)/foo",
            "/api/v2/systems/{system-id}/context-types/{context-type-id}, (/[A-Z0-9-_]+)?/api/v2/systems/([A-Z0-9-_.]+)/context-types/([A-Z0-9-_.]+)"})
    void convertPathToRegex(String input, String expected) {
        assertThat(UrlHelper.convertPathToRegex(input)).isEqualTo(expected);
    }


}