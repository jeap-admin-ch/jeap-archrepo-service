package ch.admin.bit.jeap.archrepo.docgen;

import ch.admin.bit.jeap.archrepo.metamodel.reaction.ReactionStatistics;
import lombok.Builder;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@Builder(access = PRIVATE)
public class ReactionStatisticsView {

    String triggerType;
    
    String triggerFqn;
    
    String actionType;
    
    String actionFqn;
    
    Integer count;
    
    Double median;
    
    Double percentage;
    
    String providerName;
    
    static ReactionStatisticsView of(ReactionStatistics statistics) {
        return ReactionStatisticsView.builder()
                .triggerType(statistics.getTriggerType())
                .triggerFqn(statistics.getTriggerFqn())
                .actionType(statistics.getActionType())
                .actionFqn(statistics.getActionFqn())
                .count(statistics.getCount())
                .median(statistics.getMedian())
                .percentage(statistics.getPercentage())
                .providerName(statistics.getComponent().getName())
                .build();
    }
}