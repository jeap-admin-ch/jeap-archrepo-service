package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.springframework.http.HttpHeaders.encodeBasicAuth;

@AutoConfiguration
@EnableConfigurationProperties(ReactionObserverServiceProperties.class)
@ComponentScan
@ConditionalOnProperty("reactionobserverservice.url")
public class ReactionsObserverImporterConfiguration {

    @Bean
    ReactionObserverService reactionObserverService(ReactionObserverServiceProperties properties) {
        RestClient client = RestClient.builder()
                .baseUrl(properties.getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodeBasicAuth(properties.getUsername(), properties.getPassword(), null))
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(ReactionObserverService.class);
    }
}