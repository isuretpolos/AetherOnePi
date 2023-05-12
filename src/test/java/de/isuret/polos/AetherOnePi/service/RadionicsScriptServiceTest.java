package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.domain.RadionicScript;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RadionicsScriptServiceTest {

    @Test
    public void testScript() {
        AetherOneUI p = new AetherOneUI();
        p.getDataService().init();
        RadionicsScriptService service = new RadionicsScriptService(p);
        RadionicScript script = service.executeScript("nonExistingScript");
        Assertions.assertNull(script);

        script = service.executeScript("helloWorld");
    }
}
