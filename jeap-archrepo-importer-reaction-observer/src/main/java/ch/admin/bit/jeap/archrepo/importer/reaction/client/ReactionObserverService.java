package ch.admin.bit.jeap.archrepo.importer.reaction.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface ReactionObserverService {

    @GetExchange("/api/statistics/{component}")
    List<ReactionsObservedStatisticsDto> getReactionsObservedStatistics(@PathVariable("component") String component);

    @GetExchange("/api/systems/names")
    List<String> getSystemNames();

    @GetExchange("/api/graphs/systems/{system}")
    GraphDto getSystemGraph(@PathVariable("system") String system);

    @GetExchange("/api/graphs/components/{component}")
    GraphDto getComponentGraph(@PathVariable("component") String component);

    @GetExchange("/api/graphs/messages/{message}")
    MessageGraphDto getMessageGraph(@PathVariable("message") String message);


}
