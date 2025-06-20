package ch.admin.bit.jeap.archrepo.importer.reaction.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface ReactionObserverService {

    @GetExchange("/api/statisticsV2/{component}")
    List<ReactionsObservedStatisticsV2Dto> getReactionsObservedStatistics(@PathVariable("component") String component);

}
