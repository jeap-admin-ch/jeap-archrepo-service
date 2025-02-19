package ch.admin.bit.jeap.archrepo.persistence;

import ch.admin.bit.jeap.archrepo.metamodel.ArchitectureModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ArchitectureModelRepository {

    private final TeamRepository teamRepository;
    private final SystemRepository systemRepository;

    @Value("${archrepo.openapi-base-url}")
    private String openApiBaseUrl;

    public ArchitectureModel load() {
        return ArchitectureModel.builder()
                .teams(teamRepository.findAll())
                .systems(systemRepository.findAll())
                .openApiBaseUrl(openApiBaseUrl)
                .build();
    }

    public void save(ArchitectureModel model) {
        systemRepository.saveAll(model.getSystems());
    }

}
