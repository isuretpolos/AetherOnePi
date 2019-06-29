package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.Rate;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.enums.AetherOnePins;
import de.isuret.polos.AetherOnePi.hotbits.HotbitsClient;
import de.isuret.polos.AetherOnePi.processing2.elements.AnalyseScreen;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    public static final int MAX_RATELIST_SIZE = 5000;
    public static final int MAX_HIT = 100;

    @Setter
    @Autowired
    private HotbitsClient hotbitsClient;

    @Autowired
    private PiService piService;

    public AnalysisResult getAnalysisResult(Iterable<Rate> rates) {

        AnalysisResult analysisResult = new AnalysisResult();

        if (hotbitsClient.getHotbitPackages().size() == 0) {
            System.err.println("ERROR - not enough hotbit packages! Please use the stick-pad!");
            return analysisResult;
        }

        List<Rate> rateList = new ArrayList<>();

        for (Rate rate : rates) {
            rateList.add(rate);
        }

        Map<String, Integer> ratesValues = new HashMap<>();

        int max = rateList.size() / 10;
        if (max > MAX_RATELIST_SIZE) max = MAX_RATELIST_SIZE;
        int count = 0;

        while (rateList.size() > 0) {

            int x = hotbitsClient.getInteger(0,rateList.size() - 1);
            Rate rate = rateList.remove(x);
            ratesValues.put(rate.getName(),0);

            count +=1;

            if (count >= max) {
                break;
            }
        }

        int biggestLevel = 0;
        boolean analysisFinished = false;

        while (!analysisFinished) {
            for (String rate : ratesValues.keySet()) {

                Integer energeticValue = ratesValues.get(rate);

                energeticValue += hotbitsClient.getInteger(10);

                ratesValues.put(rate, energeticValue);

                if (energeticValue > biggestLevel) {
                    biggestLevel = energeticValue;
                }

                if (biggestLevel >= MAX_HIT) {
                    analysisFinished = true;
                    break;
                }
            }

            if (piService != null) {
                piService.toggle(AetherOnePins.CONTROL);
                piService.delay(30);
            }
        }

        if (piService != null) {
            piService.high(AetherOnePins.CONTROL);
        }

        for (String rate : ratesValues.keySet()) {
            analysisResult.getRateObjects().add(new RateObject(ratesValues.get(rate),rate,0,0, 0));
        }

        AnalysisResult sortedResult = analysisResult.sort().shorten(AnalyseScreen.MAX_ENTRIES);

        return sortedResult;
    }
}
