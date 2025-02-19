package ch.admin.bit.jeap.archrepo.importer.deploymentlog.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface DeploymentlogService {

    @GetExchange("/api/environment/{environment}/components")
    List<ComponentVersionSummaryDto> getDeployedComponents(@PathVariable("environment") String environment);
}
