package ch.admin.bit.jeap.archrepo.web.rest.docsapi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

/**
 * The collaborators the docs API controllers need in a {@code @WebMvcTest}, which loads no {@code @Component}
 * beans of its own.
 */
@TestConfiguration
class DocsApiTestConfiguration {

    @Bean
    DocsApiDtoFactory docsApiDtoFactory() {
        return new DocsApiDtoFactory();
    }

    @Bean
    DocsApiEtagSupport docsApiEtagSupport(ObjectMapper objectMapper) {
        return new DocsApiEtagSupport(objectMapper);
    }

    @Bean
    DocsApiExceptionHandler docsApiExceptionHandler() {
        return new DocsApiExceptionHandler();
    }
}
