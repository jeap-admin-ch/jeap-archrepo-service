package ch.admin.bit.jeap.archrepo.web.rest.admin;

import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RelationStatus;
import ch.admin.bit.jeap.archrepo.metamodel.relation.RestApiRelation;
import ch.admin.bit.jeap.archrepo.persistence.RestApiRelationRepository;
import ch.admin.bit.jeap.archrepo.persistence.SystemRepository;
import ch.admin.bit.jeap.archrepo.persistence.TeamRepository;
import ch.admin.bit.jeap.archrepo.web.rest.model.CreateSystemDto;
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
public class ManagementController {

    private final TeamRepository teamRepository;
    private final SystemRepository systemRepository;
    private final RestApiRelationRepository restApiRelationRepository;

    @PostMapping("/system")
    public void createSystem(@RequestBody CreateSystemDto createSystemDto) {
        log.info("Store new System with name '{}'", createSystemDto.getName());

        System system = System.builder()
                .name(createSystemDto.getName())
                .description(createSystemDto.getDescription())
                .confluenceLink(createSystemDto.getConfluenceLink())
                .defaultOwner(getOrCreateTeam(createSystemDto.getTeamName()))
                .aliases(createSystemDto.getAliases())
                .build();
        systemRepository.save(system);
    }

    private Team getOrCreateTeam(String teamName) {
        log.info("Get Team with name '{}'", teamName);
        Optional<Team> team = teamRepository.findByName(teamName);

        if (team.isPresent()) {
            return team.get();
        }

        log.info("Team not found. Save a new Team with name '{}'", teamName);
        return teamRepository.save(Team.builder().name(teamName).build());

    }

    @PostMapping("/team")
    public void createTeam(String name,
                           @RequestParam(required = false) String contactAddress,
                           @RequestParam(required = false) String confluenceLink,
                           @RequestParam(required = false) String jiraLink) {
        log.info("Save Team with name '{}'", name);
        Team team = Team.builder()
                .name(name)
                .contactAddress(contactAddress)
                .confluenceLink(confluenceLink)
                .jiraLink(jiraLink)
                .build();
        teamRepository.save(team);
    }

    @DeleteMapping("/rest-api")
    @Transactional
    public ResponseEntity<String> deleteRestApi(@RequestBody @Valid DeleteRestApiDto dto) {
        log.info("Delete RestApi relation: {}", dto);
        Optional<System> system = systemRepository.findByNameContainingIgnoreCase(dto.getSystemName());
        if (system.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No such system " + dto.getSystemName());
        }

        List<RestApiRelation> relations = restApiRelationRepository.findAllByDefiningSystemAndProviderNameAndConsumerNameAndStatus(system.get(), dto.getProviderName(), dto.getConsumerName(), RelationStatus.ACTIVE);
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
