package ch.admin.bit.jeap.archrepo.web.service;

import ch.admin.bit.jeap.archrepo.docgen.DocumentationGenerator;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.persistence.ArchitectureModelRepository;
import ch.admin.bit.jeap.archrepo.persistence.OpenApiSpecRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.persistence.TeamRepository;
import ch.admin.bit.jeap.archrepo.web.rest.model.ArchRepoTestConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(ArchRepoTestConfiguration.class)
@ActiveProfiles("test")
class UpdateServiceGenerateDocumentationTest {
    @Autowired
    UpdateService generatorService;
    @MockBean
    ArchRepoImporter importer;
    @MockBean
    DocumentationGenerator generator;
    @Mock
    ArchitectureModelRepository repository;
    @MockBean
    TeamRepository teamRepository;
    @MockBean
    SystemRepository systemRepository;
    @MockBean
    OpenApiSpecRepository openApiSpecRepository;

    @Test
    void generate() {
        ArchitectureModel model = ArchitectureModel.builder().openApiBaseUrl("https://base-url").build();
        doReturn(model).when(repository).load();

        generatorService.generateDocumentation();

        verify(generator).generate(model);
    }

}
