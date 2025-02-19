package ch.admin.bit.jeap.archrepo.metamodel.system;

import ch.admin.bit.jeap.archrepo.metamodel.Importer;
import ch.admin.bit.jeap.archrepo.metamodel.System;
import ch.admin.bit.jeap.archrepo.metamodel.Team;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemComponentFactoryTest {

    private static final Importer IMPORTER = Importer.GRAFANA;
    private static final Team DEFAULT_OWNER = Team.builder()
            .name("team").build();
    private static final System SYSTEM = System.builder()
            .defaultOwner(DEFAULT_OWNER)
            .build();

    @Test
    void createSystemComponent_service() {
        SystemComponent service = SystemComponentFactory.createSystemComponent(SYSTEM, "app-service", IMPORTER);

        SystemComponent expectedService = new BackendService("app-service");

        assertComponent(SYSTEM, expectedService, service);
    }

    @Test
    void createSystemComponent_frontend() {
        SystemComponent ui = SystemComponentFactory.createSystemComponent(SYSTEM, "app-ui", IMPORTER);
        SystemComponent frontend = SystemComponentFactory.createSystemComponent(SYSTEM, "app-frontend", IMPORTER);

        SystemComponent expectedUi = new Frontend("app-ui");
        SystemComponent expectedFrontend = new Frontend("app-frontend");

        assertComponent(SYSTEM, expectedUi, ui);
        assertComponent(SYSTEM, expectedFrontend, frontend);
    }

    @Test
    void createSystemComponent_scs() {
        SystemComponent scs = SystemComponentFactory.createSystemComponent(SYSTEM, "app-scs", IMPORTER);

        SystemComponent expectedScs = new SelfContainedSystem("app-scs");

        assertComponent(SYSTEM, expectedScs, scs);
    }

    private void assertComponent(System system, SystemComponent expected, SystemComponent actual) {
        expected.setImporter(IMPORTER);
        expected.setParent(system);
        expected.setOwnedBy(DEFAULT_OWNER);

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getOwnedBy(), actual.getOwnedBy());
        assertEquals(expected.getParent(), actual.getParent());
        assertSame(system, actual.getParent());
        assertTrue(system.getSystemComponents().contains(actual));
    }
}
