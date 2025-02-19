package ch.admin.bit.jeap.archrepo.importer.prometheus;

import ch.admin.bit.jeap.archrepo.importer.prometheus.client.JeapRelation;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModelHelper;
import ch.admin.bit.jeap.archrepo.metamodel.system.BackendService;
import ch.admin.bit.jeap.archrepo.metamodel.system.SelfContainedSystem;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpMethod;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ValidatedJeapRelationImporter {

    private static final Pattern NAME_PARTS = Pattern.compile("(?<system>.+?)-(?<component>.+)");
    private static final String HTTP_TECHNOLOGY = "http";
    private static final String ACTUATOR_PATH = "/actuator";
    private static final String IGNORED_PATTERNS = "/**";
    private static final Set<String> IGNORED_HTTP_METHODS = Set.of(
            HttpMethod.OPTIONS.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.TRACE.name()
    );

    private final Logger log;

    public void importJeapRelationFromPrometheus(ArchitectureModel model, JeapRelation jeapRelation) {
        if (!validHttpRequest(jeapRelation)) {
            return;
        }

        Optional<SystemComponent> consumerComponent = ArchitectureModelHelper.findComponentByNameWithSystemPrefix(model, jeapRelation.getConsumer(), NAME_PARTS);
        Optional<SystemComponent> providerComponent = ArchitectureModelHelper.findComponentByNameWithSystemPrefix(model, jeapRelation.getProvider(), NAME_PARTS);

        if (consumerComponent.isEmpty()) {
            log.warn("Consumer {} not found", jeapRelation.getConsumer());
            return;
        }
        if (providerComponent.isEmpty()) {
            log.warn("Provider {} not found", jeapRelation.getProvider());
            return;
        }
        if (!(providerComponent.get() instanceof BackendService) && !(providerComponent.get() instanceof SelfContainedSystem)) {
            log.warn("Provider {} found, but is not a component type that can provide REST APIs", jeapRelation.getProvider());
            return;
        }

        JeapRelationImporter requestResponseImporter = new JeapRelationImporter(consumerComponent.get(), providerComponent.get());
        requestResponseImporter.importRelation(jeapRelation);
    }

    private boolean validHttpRequest(JeapRelation jeapRelation) {
        return HTTP_TECHNOLOGY.equalsIgnoreCase(jeapRelation.getTechnology()) &&
                jeapRelation.getDatapoint() != null &&
                !IGNORED_HTTP_METHODS.contains(jeapRelation.getMethod().toUpperCase()) &&
                !jeapRelation.getDatapoint().contains(ACTUATOR_PATH) &&
                !jeapRelation.getDatapoint().contains(IGNORED_PATTERNS) &&
                !mightBeZapScanRequest(jeapRelation);
    }

    private boolean mightBeZapScanRequest(JeapRelation jeapRelation) {
        String path = jeapRelation.getDatapoint();
        return path.contains("%") ||
                path.contains(";") ||
                path.contains("SYSTEMROOT") ||
                path.contains("chr(");
    }
}
