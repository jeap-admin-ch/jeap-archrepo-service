package ch.admin.bit.jeap.archrepo.web.rest.admin;

import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationStatus;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.persistence.RestApiRelationRepository;
import ch.admin.bit.jeap.archrepo.web.rest.model.DeleteRestApiDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ManagementController {

    private final RestApiRelationRepository restApiRelationRepository;

    @DeleteMapping("/rest-api")
    public ResponseEntity<String> deleteRestApi(@RequestBody @Valid DeleteRestApiDto dto) {
        log.info("Delete RestApi relation: {}", dto);

        List<RestApiRelation> relations = restApiRelationRepository.findAllByProviderNameAndConsumerNameAndStatus(dto.getProviderName(), dto.getConsumerName(), RelationStatus.ACTIVE);
        log.info("Found {} RestApi relations between {} and {}", relations.size(), dto.getProviderName(), dto.getConsumerName());
        Optional<RestApiRelation> relationOptional = relations.stream().filter(relation ->
                        dto.getPath().equalsIgnoreCase(relation.getRestApi().getPath()) &&
                                dto.getMethod().equalsIgnoreCase(relation.getRestApi().getMethod()))
                .findFirst();

        if (relationOptional.isPresent()) {
            log.info("Delete RestApi relation: {}", relationOptional.get());
            relationOptional.get().markDeleted();
            return ResponseEntity.status(HttpStatus.OK).body("Relation deleted successfully");
        }

        log.info("No relations found for method '{}' and path '{}'", dto.getMethod(), dto.getPath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No relations found");
    }
}
