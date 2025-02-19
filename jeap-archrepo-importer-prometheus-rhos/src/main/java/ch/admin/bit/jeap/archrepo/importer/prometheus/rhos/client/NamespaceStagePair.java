package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos.client;

import lombok.Value;

@Value
public class NamespaceStagePair {
    private final String namespaceName;
    private final String stageName;
}
