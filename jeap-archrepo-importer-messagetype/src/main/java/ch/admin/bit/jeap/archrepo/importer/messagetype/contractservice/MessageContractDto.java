package ch.admin.bit.jeap.archrepo.importer.messagetype.contractservice;

import ch.admin.bit.jeap.archrepo.metamodel.message.MessageContract;

import java.util.List;

public record MessageContractDto(
        String appName,
        String messageType,
        String messageTypeVersion,
        String topic,
        MessageContractRole role) {

    public MessageContract toModelObject() {
        return MessageContract.builder()
                .version(List.of(messageTypeVersion))
                .componentName(appName)
                .topic(topic)
                .build();
    }
}
