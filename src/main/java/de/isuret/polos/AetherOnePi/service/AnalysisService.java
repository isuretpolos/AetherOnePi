package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.Rate;
import de.isuret.polos.AetherOnePi.domain.VitalityObject;
import de.isuret.polos.AetherOnePi.enums.AetherOnePins;
import de.isuret.polos.AetherOnePi.hotbits.HotbitsClient;
import de.isuret.polos.AetherOnePi.processing2.elements.AnalyseScreen;
import de.isuret.polos.AetherOnePi.utils.RateUtils;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AnalysisService {

    public static final int MAX_RATELIST_SIZE = 5000;
    public static final int MAX_HIT = 100;

    @Setter
    @Autowired
    private HotbitsClient hotbitsClient;

    @Setter
    @Autowired
    private PiService piService;

    public AnalysisResult analyseRateList(Iterable<Rate> rates) {

        AnalysisResult analysisResult = new AnalysisResult();

        try {
            List<Rate> rateList = new ArrayList<>();

            for (Rate rate : rates) {
                rateList.add(rate);
            }

            rateList = shuffleRateList(rateList);

            Map<String, Integer> ratesValues = new HashMap<>();

            int max = rateList.size() / 10;
            if (max > MAX_RATELIST_SIZE) max = MAX_RATELIST_SIZE;
            int count = 0;

            /**
             * Get some rates
             */
            while (rateList.size() > 0) {

                int x = hotbitsClient.getInteger(0, rateList.size() - 1);
                Rate rate = rateList.remove(x);
                ratesValues.put(rate.getName(), 0);

                count += 1;

                if (count >= max) {
                    break;
                }
            }

            int biggestLevel = 0;
            boolean analysisFinished = false;

            /**
             * Add energetic value
             */
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
                RateUtils.insertRate(analysisResult, ratesValues, rate);
            }

            System.out.println(analysisResult);

            AnalysisResult sortedResult = analysisResult.sort().shorten(AnalyseScreen.MAX_ENTRIES);

            return sortedResult;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR - ... something gone wrong during analysis! Please use the stick-pad!");
            return analysisResult;
        }
    }

    /**
     * Shuffle the rate list once before using it, so there is no "order" which could form a typical bell curve
     * @param rateList
     * @return
     */
    private List<Rate> shuffleRateList(List<Rate> rateList) {
        List<Rate> shuffledRateList = new ArrayList<>();

        while (rateList.size() > 0) {
            shuffledRateList.add(rateList.remove(hotbitsClient.getInteger(0, rateList.size() - 1)));
        }

        return shuffledRateList;
    }

    public Integer checkGeneralVitality() {
        Map<Integer,Integer> vitalityMap = new HashMap<>();

        for (int x=0; x<101; x++) {

            vitalityMap.put(x,0);
        }

        for (int x=0; x<3456; x++) {

            Integer key = hotbitsClient.getInteger(0,100);
            Integer value = vitalityMap.get(key) + 1;

//            System.out.println(String.format("key %s value %s", key, value));
            vitalityMap.put(key,value);
        }

        List<VitalityObject> vitalityList = new ArrayList<>();

        for (int x=0; x<101; x++) {
            vitalityList.add(new VitalityObject(x,vitalityMap.get(x)));
        }

        Collections.sort(vitalityList, new Comparator<VitalityObject>() {
            @Override
            public int compare(VitalityObject o1, VitalityObject o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        return vitalityList.get(0).getValue();
    }
}
