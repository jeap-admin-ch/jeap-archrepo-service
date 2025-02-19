package ch.admin.bit.jeap.archrepo.web.rest.model;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SuppressWarnings("ClassEscapesDefinedScope")
@TestConfiguration
public class ArchRepoWebTestConfiguration {

    @Bean
    public ModelDtoFactory modelDtoFactory() {
        return new ModelDtoFactory();
    }


}
