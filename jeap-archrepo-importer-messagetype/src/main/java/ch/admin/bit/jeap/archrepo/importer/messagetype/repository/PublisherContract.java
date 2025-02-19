package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class PublisherContract {
    @NonNull String service;
    String system;
    String topic;
    @NonNull List<String> publishedVersions;
}
