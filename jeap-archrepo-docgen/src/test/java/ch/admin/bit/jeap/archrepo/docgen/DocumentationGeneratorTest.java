package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.docgen.plantuml.PlantUmlRenderer;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
class DocumentationGeneratorTest {

    private static final String ROOT_PAGE_NAME = "rootPageName";

    @Autowired
    ApplicationContext applicationContext;

    @Mock
    ConfluenceAdapter confluenceAdapterMock;

    private DocumentationGenerator documentationGenerator;

    @Test
    void generate() {
        // given
        String rootPageId = "rootPageId";
        String systemName = "system";
        System system = System.builder()
                .name(systemName)
                .build();
        ArchitectureModel model = ArchitectureModel.builder()
                .systems(List.of(system))
                .build();
        doReturn(rootPageId).when(confluenceAdapterMock).getPageByName(ROOT_PAGE_NAME);
        doAnswer(args -> args.getArgument(1).equals(systemName) ? rootPageId : UUID.randomUUID().toString())
                .when(confluenceAdapterMock).addOrUpdatePageUnderAncestor(anyString(), anyString(), anyString());

        // when
        documentationGenerator.generate(model);

        // then
        verify(confluenceAdapterMock)
                .addOrUpdatePageUnderAncestor(eq(rootPageId), eq(systemName + " (System)"), anyString());
    }

    @BeforeEach
    void setUp() {
        DocumentationGeneratorConfiguration generatorConfig = new DocumentationGeneratorConfiguration();
        TemplateRenderer templateRenderer = new TemplateRenderer(generatorConfig.templateEngine(applicationContext), new PlantUmlRenderer());
        DocumentationGeneratorConfluenceProperties props = new DocumentationGeneratorConfluenceProperties();
        props.setRootPageName(ROOT_PAGE_NAME);
        documentationGenerator = new DocumentationGenerator(confluenceAdapterMock, templateRenderer, props);
    }
}
