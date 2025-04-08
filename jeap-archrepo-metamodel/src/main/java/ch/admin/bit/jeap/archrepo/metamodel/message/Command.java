package ch.admin.bit.jeap.archrepo.metamodel.message;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Entity
@DiscriminatorValue("COMMAND")
public class Command extends MessageType {

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "messageSender")
    private List<MessageContract> senderContracts;


    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "messageReceiver")
    private List<MessageContract> receiverContracts;

    public List<MessageContract> getSenderContracts() {
        return senderContracts == null ? emptyList() : senderContracts;
    }

    public List<MessageContract> getReceiverContracts() {
        return receiverContracts == null ? emptyList() : receiverContracts;
    }

    public void addReceiverContract(MessageContract messageContract) {
        if (receiverContracts == null) {
            receiverContracts = new ArrayList<>();
        }
        messageContract.setMessageReceiver(this);
        receiverContracts.add(messageContract);
    }

    public void addSenderContract(MessageContract messageContract) {
        if (senderContracts == null) {
            senderContracts = new ArrayList<>();
        }
        messageContract.setMessageSender(this);
        senderContracts.add(messageContract);
    }

    @Override
    public Set<String> getComponentNamesWithContract() {
        Stream<MessageContract> senderStream = Optional.ofNullable(senderContracts).map(List::stream).orElse(Stream.empty());
        Stream<MessageContract> receiverStream = Optional.ofNullable(receiverContracts).map(List::stream).orElse(Stream.empty());
        return Stream.concat(senderStream, receiverStream)
                .map(MessageContract::getComponentName)
                .collect(Collectors.toSet());
    }
}
