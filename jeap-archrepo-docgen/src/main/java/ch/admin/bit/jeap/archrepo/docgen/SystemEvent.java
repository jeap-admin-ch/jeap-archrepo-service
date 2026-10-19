package ch.admin.bit.jeap.archrepo.docgen;

import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
public class SystemEvent {
    String name;
    Set<String> providerNames;
    Set<String> consumerNames;

    public List<String> getProviderNames() {
        return providerNames.stream()
                .sorted()
                .toList();
    }

    public List<String> getConsumerNames() {
        return consumerNames.stream()
                .sorted()
                .toList();
    }
}
