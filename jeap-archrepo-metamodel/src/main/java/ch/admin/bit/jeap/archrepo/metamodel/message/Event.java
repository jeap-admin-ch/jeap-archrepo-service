package ch.admin.bit.jeap.archrepo.metamodel.message;

import com.google.common.collect.Streams;
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
@DiscriminatorValue("EVENT")
public class Event extends MessageType {

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "messagePublisher")
    private List<MessageContract> publisherContracts;


    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "messageConsumer")
    private List<MessageContract> consumerContracts;

    public List<MessageContract> getPublisherContracts() {
        return publisherContracts == null ? emptyList() : publisherContracts;
    }

    public List<MessageContract> getConsumerContracts() {
        return consumerContracts == null ? emptyList() : consumerContracts;
    }

    public void addConsumerContract(MessageContract messageContract) {
        if (consumerContracts == null) {
            consumerContracts = new ArrayList<>();
        }
        messageContract.setMessageConsumer(this);
        consumerContracts.add(messageContract);
    }

    public void addPublisherContract(MessageContract messageContract) {
        if (publisherContracts == null) {
            publisherContracts = new ArrayList<>();
        }
        messageContract.setMessagePublisher(this);
        publisherContracts.add(messageContract);
    }

    @Override
    public Set<String> getComponentNamesWithContract() {
        Stream<MessageContract> publisherStream = Optional.ofNullable(publisherContracts).map(List::stream).orElse(Stream.empty());
        Stream<MessageContract> consumerStream = Optional.ofNullable(consumerContracts).map(List::stream).orElse(Stream.empty());
        return Streams.concat(publisherStream, consumerStream)
                .map(MessageContract::getComponentName)
                .collect(Collectors.toSet());
    }
}
