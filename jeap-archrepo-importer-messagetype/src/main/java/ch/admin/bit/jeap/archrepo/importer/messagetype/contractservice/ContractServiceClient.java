package ch.admin.bit.jeap.archrepo.importer.messagetype.contractservice;

import ch.admin.bit.jeap.archrepo.importer.messagetype.MessageTypeImporterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class ContractServiceClient {

    private final RestClient restClient;

    public ContractServiceClient(MessageTypeImporterProperties properties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(properties.getMessageContractServiceUri())
                .build();
    }

    public List<MessageContractDto> getMessageContracts(String environment) {
        MessageContractDto[] result = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("env", environment)
                        .build()
                )
                .retrieve()
                .body(MessageContractDto[].class);


        if (result != null) {
            return Arrays.asList(result);
        }

        log.error("Response of request for environment {} is null",  environment);
        throw new IllegalStateException("Response is null");

    }

}
