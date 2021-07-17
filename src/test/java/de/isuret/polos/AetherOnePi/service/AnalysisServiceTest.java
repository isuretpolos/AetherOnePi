package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.Rate;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.hotbits.HotbitsClient;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalysisServiceTest {

    @Test
    public void testClarkeAnalysis() {
        AnalysisService analysisService = new AnalysisService();
        AnalysisResult analysisResult = new AnalysisResult();
        RateObject rateObjectCina = new RateObject();
        rateObjectCina.setNameOrRate("Cina");
        analysisResult.getRateObjects().add(rateObjectCina);
        RateObject rateObjectAntimon = new RateObject();
        rateObjectAntimon.setNameOrRate("Antimonium Tartaricum");
        analysisResult.getRateObjects().add(rateObjectAntimon);
        analysisService.analyzeClarke("Cina",analysisResult);
    }

    @Test
    public void testAnalysisService() throws IOException {
        AnalysisService analysisService = new AnalysisService();
        analysisService.setHotbitsClient(new HotbitsClient());
        analysisService.setPiService(new PiService());

        DataService dataService = new DataService();
        dataService.init();
        Iterable<Rate> rates = dataService.findAllBySourceName("HOMEOPATHY_COMPLETE_NO_RATES.txt");
        AnalysisResult analysisResult = analysisService.analyseRateList(rates);

        final List<Integer> counter = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            System.out.println(analysisService.checkGeneralVitality());
        }

        for (int i = 0; i < analysisResult.getRateObjects().size(); i++) {
            final RateObject rateObject = analysisResult.getRateObjects().get(i);
            (new Thread() {
                public void run() {
                    counter.add(0);
                    Integer gv = analysisService.checkGeneralVitality();
                    rateObject.setGv(gv);
                    counter.remove(0);
                }
            }).start();
        }

        while (counter.size() > 0) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("========================================");
        System.out.println(analysisResult.toString().replaceAll(",","\n"));

        Assert.assertEquals(0, counter.size());

        System.out.println("========================================");
        analysisResult = analysisService.checkGeneralVitalityForAnalysis(analysisResult);
        System.out.println(analysisResult.toString().replaceAll(",","\n"));
    }
}
