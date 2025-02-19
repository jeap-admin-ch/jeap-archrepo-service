package ch.admin.bit.jeap.archrepo.importer.prometheus.rhos;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RhosRhosSystemNameExtractorTest {

    private RhosSystemNameExtractor rhosSystemNameExtractor = new RhosSystemNameExtractor();

    @Test
    void extractSystemNameDoReturnSystemNameWhenRealNamespace() {
        Optional<String> systemName = rhosSystemNameExtractor.extractSystemName("bit-jme-d");
        assertEquals("jme", systemName.get());
    }

    @Test
    void extractSystemNameDoReturnNullWhenNotExpectedPattern() {
        Optional<String> systemName = rhosSystemNameExtractor.extractSystemName("bitjmed");
        assertTrue(systemName.isEmpty());
    }

}