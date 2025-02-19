package ch.admin.bit.jeap.archrepo.importer.messagetype.contractservice;

import ch.admin.bit.jeap.archrepo.importer.messagetype.MessageTypeImporterProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ContractServiceClient {

    private final RestClient restClient;

    public ContractServiceClient(MessageTypeImporterProperties properties, RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(properties.getMessageContractServiceUri())
                .build();
    }

    public List<MessageContractDto> getMessageContracts() {
        //noinspection DataFlowIssue
        return List.of(restClient.get()
                .retrieve()
                .body(MessageContractDto[].class)
        );
    }
}
