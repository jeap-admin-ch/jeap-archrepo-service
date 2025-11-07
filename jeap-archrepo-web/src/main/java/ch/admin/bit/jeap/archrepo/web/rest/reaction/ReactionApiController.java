package ch.admin.bit.jeap.archrepo.web.rest.reaction;

import ch.admin.bit.jeap.archrepo.persistence.ComponentGraphRepository;
import ch.admin.bit.jeap.archrepo.persistence.ReactionLastModifiedAt;
import ch.admin.bit.jeap.archrepo.web.rest.model.ReactionLastModifiedAtDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reactions")
@RequiredArgsConstructor
@Slf4j
class ReactionApiController {

    private final ComponentGraphRepository componentGraphRepository;

    @Transactional(readOnly = true)
    @GetMapping(value = "/components", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the last modified date of observed reactions for all system components.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public List<ReactionLastModifiedAtDto> getComponentsWithReactions() {
        log.debug("Retrieving all components with observed reactions...");

        return componentGraphRepository.getMaxCreatedAndModifiedAtList().stream()
                .map(row -> new ReactionLastModifiedAtDto(
                        row.getComponent(),
                        row.getMaxCreatedAt().isAfter(row.getMaxModifiedAt())
                                ? row.getMaxCreatedAt()
                                : row.getMaxModifiedAt()
                ))
                .toList();
    }
}