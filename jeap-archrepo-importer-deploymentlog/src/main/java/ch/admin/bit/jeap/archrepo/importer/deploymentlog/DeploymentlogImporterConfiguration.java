package ch.admin.bit.jeap.archrepo.importer.deploymentlog;

import ch.admin.bit.jeap.rest.tracing.AddSenderSystemHeader;
import ch.admin.bit.jeap.archrepo.importer.deploymentlog.client.DeploymentlogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Base64;

@AutoConfiguration
@EnableConfigurationProperties(DeploymentlogProperties.class)
@ConditionalOnProperty("deploymentlog.url")
public class DeploymentlogImporterConfiguration {

    @Bean
    DeploymentlogService deploymentlogService(DeploymentlogProperties properties,
                                              @Value("${spring.application.name}") String applicationName) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getUrl())
                .defaultHeader(AddSenderSystemHeader.APPLICATION_NAME_HEADER, applicationName)
                .defaultHeader(HttpHeaders.AUTHORIZATION, encodeBasicAuth(properties))
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(DeploymentlogService.class);
    }

    private String encodeBasicAuth(DeploymentlogProperties properties) {
        String userPassword = properties.getUsername() + ":" + properties.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(userPassword.getBytes());
    }

    @Bean
    DeploymentlogSystemComponentImporter deploymentlogSystemComponentImporter(DeploymentlogService service) {
        return new DeploymentlogSystemComponentImporter(service);
    }
}
