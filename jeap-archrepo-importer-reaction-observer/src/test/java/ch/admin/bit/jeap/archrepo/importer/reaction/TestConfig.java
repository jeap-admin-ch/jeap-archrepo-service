package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.metamodel.message.MessageGraph;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemGraph;
import ch.admin.bit.jeap.archrepo.persistence.MessageGraphRepository;
import ch.admin.bit.jeap.archrepo.persistence.ComponentGraphRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemGraphRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
class TestConfig {
    @Bean
    @Primary
    public SystemGraphRepository systemGraphRepository() {
        SystemGraphRepository mockRepository = mock(SystemGraphRepository.class);
        when(mockRepository.save(any(SystemGraph.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return mockRepository;
    }

    @Bean
    @Primary
    public ComponentGraphRepository serviceGraphRepository() {
        return mock(ComponentGraphRepository.class);
    }

    @Bean
    @Primary
    public MessageGraphRepository messageGraphRepository() {
        MessageGraphRepository mockRepository = mock(MessageGraphRepository.class);
        when(mockRepository.save(any(MessageGraph.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return mockRepository;
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
