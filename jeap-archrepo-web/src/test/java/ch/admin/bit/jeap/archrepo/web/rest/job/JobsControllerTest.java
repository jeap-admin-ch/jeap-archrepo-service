package ch.admin.bit.jeap.archrepo.web.rest.job;

import ch.admin.bit.jeap.archrepo.web.service.UpdateService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JobsControllerTest {

    @Test
    void updateModelIsTheOnlySupportedJob() {
        UpdateService updateService = mock(UpdateService.class);
        JobsController controller = new JobsController(updateService);

        controller.triggerUpdate(new JobDto(JobType.UPDATE_MODEL));

        assertThat(JobType.values()).containsExactly(JobType.UPDATE_MODEL);
        verify(updateService).updateModel();
    }
}
