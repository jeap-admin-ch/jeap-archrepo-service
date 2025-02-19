package ch.admin.bit.jeap.archrepo.importer.pactbroker.client;

import au.com.dius.pact.core.support.Auth;
import au.com.dius.pact.core.support.HttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class PactBrokerConfig {

    @Bean
    public PactBrokerClient pactBrokerClient(PactBrokerProperties props) {
        Auth auth = null;
        if (props.getUsername() != null) {
            auth = new Auth.BasicAuthentication(props.getUsername(), props.getPassword());
        }
        String pactBrokerUrl = props.getUrl();
        CloseableHttpClient httpClient = HttpClient.INSTANCE
                .newHttpClient(auth, URI.create(pactBrokerUrl), 0, 0, true).component1();
        return new PactBrokerClient(httpClient, pactBrokerUrl);
    }
}
