package ch.admin.bit.jeap.archrepo.docgen;

import org.sahli.asciidoc.confluence.publisher.client.http.ConfluenceClient;
import org.sahli.asciidoc.confluence.publisher.client.http.ConfluenceRestClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;

@AutoConfiguration
@ComponentScan
class DocumentationGeneratorConfiguration {

    private static final String CV_TEMPLATE_PATH = "/template/documentation/";

    @Bean
    ConfluenceClient confluenceClient(DocumentationGeneratorConfluenceProperties props) {
        return new ConfluenceRestClient(props.getUrl(), true, null, props.getUsername(), props.getPassword());
    }

    @Bean
    ConfluenceAdapter confluenceAdapter(ConfluenceClient confluenceClient, DocumentationGeneratorConfluenceProperties props) {
        if (props.isMockConfluenceClient()) {
            return new ConfluenceAdapterMock();
        } else {
            return new ConfluenceAdapterImpl(confluenceClient, props);
        }
    }

    @Bean
    SpringResourceTemplateResolver templateResolver(ApplicationContext applicationContext) {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(applicationContext);
        templateResolver.setPrefix("classpath:" + CV_TEMPLATE_PATH);
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
        templateResolver.setTemplateMode(TemplateMode.HTML);
        return templateResolver;
    }

    @Bean
    SpringTemplateEngine templateEngine(ApplicationContext applicationContext) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver(applicationContext));
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }
}
