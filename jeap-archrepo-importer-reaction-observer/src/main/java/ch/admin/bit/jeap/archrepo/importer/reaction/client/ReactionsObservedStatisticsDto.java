package ch.admin.bit.jeap.archrepo.importer.reaction.client;

import java.util.List;
import java.util.Map;

public record ReactionsObservedStatisticsDto(String component, String triggerType, String triggerFqn, List<Action> actions, long count, double median, Double percentage, Map<String, String> triggerProperties) {
}
