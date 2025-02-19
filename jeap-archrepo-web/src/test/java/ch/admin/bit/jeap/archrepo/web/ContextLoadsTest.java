package ch.admin.bit.jeap.archrepo.web;

import ch.admin.bit.jeap.archrepo.web.service.UpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = ArchRepoApplication.class)
@ActiveProfiles("test")
class ContextLoadsTest {

    @Autowired
    private UpdateService updateService;

    @Test
    void contextLoads() {
        assertNotNull(updateService);
    }
}
