package ch.admin.bit.jeap.archrepo.metamodel.system;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SystemComponentTest {

    @Test
    void isObsolete_importedSince15Days_returnsTrue() {
        SystemComponent backendService = BackendService.builder().name("test").build();
        backendService.setLastSeenFromDate(ZonedDateTime.now().minusDays(15));
        assertThat(backendService.isObsolete()).isTrue();
    }

    @Test
    void isObsolete_importedSince13Days_returnsFalse() {
        SystemComponent backendService = BackendService.builder().name("test").build();
        backendService.setLastSeenFromDate(ZonedDateTime.now().minusDays(13));
        assertThat(backendService.isObsolete()).isFalse();
    }

}