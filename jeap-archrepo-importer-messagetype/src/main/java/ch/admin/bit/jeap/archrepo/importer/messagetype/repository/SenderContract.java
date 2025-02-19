package ch.admin.bit.jeap.archrepo.importer.messagetype.repository;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class SenderContract {
    @NonNull String service;
    @NonNull String system;
    @NonNull List<String> versions;
    String topic;
}
