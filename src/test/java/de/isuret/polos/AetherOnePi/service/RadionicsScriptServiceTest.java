package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.domain.RadionicScript;
import de.isuret.polos.AetherOnePi.hotbits.HotbitsClient;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RadionicsScriptServiceTest {

    @Test
    public void testScript() {
        AetherOneUI p = new AetherOneUI();
        p.getDataService().init();
        AnalysisService analysisService = new AnalysisService();
        analysisService.setHotbitsClient(new HotbitsClient());
        p.setAnalyseService(analysisService);
        RadionicsScriptService service = new RadionicsScriptService(p);
        RadionicScript script = service.executeScript("nonExistingScript");
        Assertions.assertNull(script);

        script = service.executeScript("helloWorld");
        Assertions.assertNotNull(script);
        Assertions.assertEquals("100",script.getVariables().get("GV"),"Variable GV should be 100");
    }
}
