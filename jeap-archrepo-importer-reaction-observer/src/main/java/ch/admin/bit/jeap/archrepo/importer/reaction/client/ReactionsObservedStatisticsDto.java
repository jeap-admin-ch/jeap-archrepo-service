package ch.admin.bit.jeap.archrepo.importer.reaction.client;

import java.util.Map;

public record ReactionsObservedStatisticsDto(String component, String triggerType, String triggerFqn, String actionType, String actionFqn, long count, double median, double percentage, Map<String, String> triggerProperties, Map<String, String> actionProperties) {
}
