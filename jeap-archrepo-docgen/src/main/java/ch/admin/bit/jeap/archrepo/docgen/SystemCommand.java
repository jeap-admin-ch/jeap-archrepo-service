package ch.admin.bit.jeap.archrepo.docgen;

import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
public class SystemCommand {
    String name;
    Set<String> senderNames;
    Set<String> receiverNames;

    public List<String> getSenderNames() {
        return senderNames.stream()
                .sorted()
                .toList();
    }

    public List<String> getReceiverNames() {
        return receiverNames.stream()
                .sorted()
                .toList();
    }
}
