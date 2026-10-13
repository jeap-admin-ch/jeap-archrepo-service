package ch.admin.bit.jeap.archrepo.importer.deploymentlog.client;

import java.time.ZonedDateTime;

public record ComponentVersionSummaryDto(String componentName, String version, boolean undeployed,
                                          ZonedDateTime undeployedAt) {
}
