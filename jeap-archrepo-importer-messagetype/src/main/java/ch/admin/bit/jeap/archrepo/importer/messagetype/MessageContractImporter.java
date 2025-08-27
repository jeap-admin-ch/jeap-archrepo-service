package ch.admin.bit.jeap.archrepo.importer.messagetype;


import ch.admin.bit.jeap.archrepo.importer.messagetype.contractservice.ContractServiceClient;
import ch.admin.bit.jeap.archrepo.importer.messagetype.contractservice.MessageContractDto;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.message.Command;
import ch.admin.bit.jeap.archrepo.metamodel.message.Event;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageContract;
import ch.admin.bit.jeap.archrepo.metamodel.message.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageContractImporter {

    private final ContractServiceClient contractServiceClient;

    public void importMessageContracts(ArchitectureModel architectureModel, String environment) {
        contractServiceClient.getMessageContracts(environment).forEach(mc -> {
            String messageTypeName = mc.messageType();
            architectureModel.findMessageType(messageTypeName).ifPresent(messageType ->
                    importMessageContract(mc, messageType));
        });
    }

    private void importMessageContract(MessageContractDto messageContractDto, MessageType messageType) {
        if (messageType instanceof Event event) {
            importEventContract(event, messageContractDto);
        } else if (messageType instanceof Command command) {
            importCommandContract(command, messageContractDto);
        }
    }

    private void importEventContract(Event event, MessageContractDto messageContractDto) {
        MessageContract messageContract = messageContractDto.toModelObject();
        switch (messageContractDto.role()) {
            case CONSUMER -> event.addConsumerContract(messageContract);
            case PRODUCER -> event.addPublisherContract(messageContract);
        }
    }

    private void importCommandContract(Command command, MessageContractDto messageContractDto) {
        MessageContract messageContract = messageContractDto.toModelObject();
        switch (messageContractDto.role()) {
            case CONSUMER -> command.addReceiverContract(messageContract);
            case PRODUCER -> command.addSenderContract(messageContract);
        }
    }
}
