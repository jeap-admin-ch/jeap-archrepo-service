package ch.admin.bit.jeap.archrepo.importer.reaction.client;

import java.util.Map;

public record ActionDto(String actionType, String actionFqn, Map<String, String> actionProperties) {
}
