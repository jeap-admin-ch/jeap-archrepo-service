package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class EventContracts {
    static final EventContracts NONE = new EventContracts(List.of(), List.of());

    @NonNull
    List<PublisherContract> publishers;

    @NonNull
    List<SubscriberContract> subscribers;
}
