package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class SubscriberContract {
    @NonNull String system;
    @NonNull String service;
    String topic;
    @NonNull List<String> subscribedVersions;
}
