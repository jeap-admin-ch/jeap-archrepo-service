package ch.admin.bit.jeap.archrepo.web.rest.job;

import ch.admin.bit.jeap.archrepo.web.service.UpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "jobs", description = "Architecture repository jobs")
@Slf4j
public class JobsController {

    private final UpdateService updateService;

    @PostMapping
    @Operation(
            summary = "Create a new job",
            description = "Create a new job to trigger Documentation Generation, Documentation Deletion (do not use on prod space!) oder Model Update manually")
    @ApiResponse(responseCode = "200", description = "Job successfully created and started asynchronously")
    @Async
    public void triggerUpdate(@RequestBody JobDto jobDto) {
        switch (jobDto.getType()) {
            case GENERATE_DOC -> updateService.generateDocumentation();
            case UPDATE_MODEL -> updateService.updateModel();
        }
    }

    @PostMapping("/import/{importerName}")
    @Operation(summary = "Run a specific import job")
    @ApiResponse(responseCode = "200", description = "Job successfully created and started asynchronously")
    @Async
    public void runImporter(@PathVariable String importerName) {
        updateService.runImporter(importerName);
    }
}
