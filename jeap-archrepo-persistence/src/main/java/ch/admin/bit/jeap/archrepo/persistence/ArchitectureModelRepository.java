package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ArchitectureModelRepository {

    private final TeamRepository teamRepository;
    private final SystemRepository systemRepository;
    private final EntityManager entityManager;

    @Value("${archrepo.openapi-base-url}")
    private String openApiBaseUrl;

    public ArchitectureModel load() {
        return ArchitectureModel.builder()
                .teams(teamRepository.findAll())
                .systems(systemRepository.findAll())
                .openApiBaseUrl(openApiBaseUrl)
                .build();
    }

    /**
     * Saves the model, then deletes what it records as removed - components and their artifacts (which are not
     * left to orphan removal, see {@code System.systemComponents}), then systems, then teams - flushing between
     * the destructive steps so that a row is only deleted once nothing still references it. Saving again is
     * harmless: what has already been deleted is skipped.
     */
    public void save(ArchitectureModel model) {
        teamRepository.saveAll(notManaged(model.getTeams()));
        systemRepository.saveAll(notManaged(model.getSystems()));
        systemRepository.flush();
        // Artifacts reference their component, so they go first.
        deleteIfManaged(model.getRemovedArtifacts());
        deleteIfManaged(model.getRemovedSystemComponents());
        systemRepository.flush();
        systemRepository.deleteAll(model.getRemovedSystems());
        systemRepository.flush();
        teamRepository.deleteAll(model.getRemovedTeams());
        teamRepository.flush();
    }

    /**
     * Only new and detached aggregates go through {@code saveAll} (persist, merge). A loaded aggregate is still
     * managed in the update's transaction: the flush writes its changes and persists its new children by cascade,
     * as the very instances the model holds. Merging it instead would copy each new child into the persistence
     * context - once per aggregate that references it, so a new REST API with a new relation in another system
     * was merged twice, and the second merge copied its unset created_at onto the managed copy.
     */
    private <T> List<T> notManaged(List<T> aggregates) {
        return aggregates.stream().filter(aggregate -> !entityManager.contains(aggregate)).toList();
    }

    /** Only managed entities have a row: one added and removed again in the same update never had one. */
    private void deleteIfManaged(List<?> entities) {
        entities.stream().filter(entityManager::contains).forEach(entityManager::remove);
    }
}
