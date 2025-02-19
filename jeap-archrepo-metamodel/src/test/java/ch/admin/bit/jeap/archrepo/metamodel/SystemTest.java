package ch.admin.bit.jeap.archrepo.metamodel;

import ch.admin.bit.jeap.archrepo.metamodel.system.Frontend;
import ch.admin.bit.jeap.archrepo.metamodel.system.SystemComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SystemTest {

    private System system;
    private Frontend frontend;

    @Test
    void addSystemComponent_throwsOnDuplicateName() {
        Frontend frontendToTest = Frontend.builder().name(frontend.getName()).build();
        assertThrows(IllegalArgumentException.class, () -> system.addSystemComponent(frontendToTest));
    }

    @Test
    void findSystemComponent() {
        Optional<SystemComponent> found = system.findSystemComponent("Frontend");
        Optional<SystemComponent> notFound = system.findSystemComponent("foo");

        assertTrue(found.isPresent());
        assertSame(frontend, found.get());
        assertTrue(notFound.isEmpty());
    }

    @BeforeEach
    void buildModel() {
        frontend = Frontend.builder().name("frontend").build();

        system = System.builder()
                .name("system1")
                .systemComponents(List.of(frontend))
                .build();
    }

}
