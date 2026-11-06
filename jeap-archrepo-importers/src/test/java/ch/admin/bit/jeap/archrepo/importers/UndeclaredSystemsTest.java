package ch.admin.bit.jeap.archrepo.importers;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UndeclaredSystemsTest {

    @Test
    void describe_listsSystemsAndElementsSortedAndWithoutDuplicates() {
        UndeclaredSystems undeclaredSystems = new UndeclaredSystems();
        undeclaredSystems.skip("terraform", "terraform-aws-jeap-blueprint");
        undeclaredSystems.skip("terraform", "terraform-aws-jeap-blueprint");
        undeclaredSystems.skip("ausbildung", "ausbildung-op1-ui");
        undeclaredSystems.skip("ausbildung", "ausbildung-cmp-ui");
        undeclaredSystems.skip("amia");

        assertThat(undeclaredSystems.describe()).isEqualTo(
                "amia, ausbildung [ausbildung-cmp-ui, ausbildung-op1-ui], terraform [terraform-aws-jeap-blueprint]");
    }

    @Test
    void skipSystem_keepsElementsAlreadyRecordedForIt() {
        UndeclaredSystems undeclaredSystems = new UndeclaredSystems();
        undeclaredSystems.skip("amia", "amia-portal-scs");
        undeclaredSystems.skip("amia");

        assertThat(undeclaredSystems.describe()).isEqualTo("amia [amia-portal-scs]");
    }

    @Test
    void logSummary_logsOneWarn() {
        Logger log = mock(Logger.class);
        UndeclaredSystems undeclaredSystems = new UndeclaredSystems();
        undeclaredSystems.skip("amia", "amia-portal-scs");
        undeclaredSystems.skip("catalogi", "catalogi-datahub-gms");

        undeclaredSystems.logSummary(log, "Deployment log");

        verify(log).warn("{}: skipped {} systems not declared in archrepo.model: {}",
                "Deployment log", 2, "amia [amia-portal-scs], catalogi [catalogi-datahub-gms]");
    }

    @Test
    void logSummary_logsNothingWhenNothingWasSkipped() {
        Logger log = mock(Logger.class);

        new UndeclaredSystems().logSummary(log, "Deployment log");

        verifyNoInteractions(log);
    }

    @Test
    void isEmpty() {
        UndeclaredSystems undeclaredSystems = new UndeclaredSystems();
        assertThat(undeclaredSystems.isEmpty()).isTrue();
        undeclaredSystems.skip("amia");
        assertThat(undeclaredSystems.isEmpty()).isFalse();
    }

}
