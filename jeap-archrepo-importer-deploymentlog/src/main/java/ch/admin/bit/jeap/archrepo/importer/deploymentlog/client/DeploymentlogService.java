package ch.admin.bit.jeap.archrepo.importer.deploymentlog.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface DeploymentlogService {

    /**
     * Also asks for components that were undeployed, so that the importer can tombstone rather than delete them.
     */
    @GetExchange("/api/environment/{environment}/components?includeUndeployed=true")
    List<ComponentVersionSummaryDto> getDeployedComponents(@PathVariable("environment") String environment);
}
