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

    Integer rowSpan;

    Integer multipleActionsRowSpan;

    static ReactionStatisticsView of(ReactionStatistics statistics, String actionType, String actionFqn, Integer rowSpan, Integer multipleActionsRowSpan) {
        return ReactionStatisticsView.builder()
                .triggerType(statistics.getTriggerType())
                .triggerFqn(statistics.getTriggerFqn())
                .actionType(actionType)
                .actionFqn(actionFqn)
                .count(statistics.getCount())
                .median(statistics.getMedian())
                .percentage(statistics.getPercentage())
                .providerName(statistics.getComponent().getName())
                .multipleActionsRowSpan(multipleActionsRowSpan)
                .rowSpan(rowSpan)
                .build();
    }
}
