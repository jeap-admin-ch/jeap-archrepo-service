package ch.admin.bit.jeap.archrepo.importer.reaction;

import ch.admin.bit.jeap.archrepo.importer.reaction.client.Action;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionObserverService;
import ch.admin.bit.jeap.archrepo.importer.reaction.client.ReactionsObservedStatisticsV2Dto;
import ch.admin.bit.jeap.archrepo.importers.ArchRepoImporter;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.reaction.ActionEntity;
import ch.admin.bit.jeap.archrepo.metamodel.reaction.ReactionStatistics;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import ch.admin.bit.jeap.archrepo.persistence.ReactionStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
class ReactionsObservedStatisticsImporter implements ArchRepoImporter {

    private final ReactionObserverService reactionObserverService;
    private final ReactionStatisticsRepository reactionStatisticsRepository;

    @Override
    public void importIntoModel(ArchitectureModel model) {
        log.info("Getting statistics from Reaction Observer Service...");
        reactionStatisticsRepository.deleteAll();
        Map<String, String> allSystemComponentNamesWithSystemName = model.getAllSystemComponentNamesWithSystemName();
        allSystemComponentNamesWithSystemName.forEach((component, system) -> {
            List<ReactionsObservedStatisticsV2Dto> statistics = reactionObserverService.getReactionsObservedStatistics(component);

            if (!statistics.isEmpty()) {
                log.trace("Found {} observed statistics for component {} in system {}", statistics.size(), component, system);

                model.findSystem(system).flatMap(s -> s.findSystemComponent(component)).ifPresent(systemComponent -> {
                    for (ReactionsObservedStatisticsV2Dto statisticsDto : statistics) {
                        ReactionStatistics reactionStatistics = toEntity(statisticsDto, systemComponent);
                        systemComponent.addReactionStatistics(reactionStatistics);
                        reactionStatisticsRepository.save(reactionStatistics);
                    }
                });
            } else {
                log.trace("No reaction statistics found for component {} in system {}", component, system);
            }
        });
    }

    private ReactionStatistics toEntity(ReactionsObservedStatisticsV2Dto statisticsDto, SystemComponent component) {
        ReactionStatistics reactionStatistics = ReactionStatistics.builder()
                .component(component)
                .triggerType(statisticsDto.triggerType())
                .triggerFqn(statisticsDto.triggerFqn())
                .count((int) statisticsDto.count())
                .median(statisticsDto.median())
                .percentage(statisticsDto.percentage())
                .build();
        for (Action action : statisticsDto.actions()) {
            ActionEntity actionEntity = ActionEntity.builder()
                    .reactionStatistics(reactionStatistics)
                    .actionType(action.actionType())
                    .actionFqn(action.actionFqn())
                    .build();
            reactionStatistics.addAction(actionEntity);
        }
        return reactionStatistics;
    }
}
