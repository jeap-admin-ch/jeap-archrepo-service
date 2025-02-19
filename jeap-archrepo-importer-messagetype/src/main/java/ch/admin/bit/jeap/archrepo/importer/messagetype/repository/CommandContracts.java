package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class CommandContracts {
    static final CommandContracts NONE = new CommandContracts(List.of(), List.of());

    @NonNull
    List<SenderContract> senders;

    @NonNull
    List<ReceiverContract> receivers;
}
