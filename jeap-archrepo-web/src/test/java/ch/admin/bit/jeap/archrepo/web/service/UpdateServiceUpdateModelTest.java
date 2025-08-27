package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.docgen.DocumentationGenerator;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.persistence.OpenApiSpecRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.persistence.TeamRepository;
import ch.admin.bit.jeap.archrepo.web.rest.model.ArchRepoTestConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"archrepo-config.environment=dev"})
@Import(ArchRepoTestConfiguration.class)
@ActiveProfiles("test")
class UpdateServiceUpdateModelTest {
    private static final String SYSTEM_NAME = "test";
    @Autowired
    UpdateService service;
    @MockitoBean
    ArchRepoImporter importer;
    @MockitoBean
    DocumentationGenerator documentationGenerator;
    @MockitoBean
    TeamRepository teamRepository;
    @MockitoBean
    SystemRepository systemRepository;
    @MockitoBean
    OpenApiSpecRepository openApiSpecRepository;

    @Captor
    ArgumentCaptor<List<System>> systemCaptor;

    @Test
    void updateModel() {
        when(systemRepository.findAll()).thenReturn(List.of(System.builder().name(SYSTEM_NAME).build()));

        service.updateModel();

        verify(systemRepository).saveAll(systemCaptor.capture());

        assertThat(systemCaptor.getValue()).hasSize(1);
        assertThat(systemCaptor.getValue().getFirst().getName()).isEqualTo(SYSTEM_NAME);
    }

}
