package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.Rate;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.domain.VitalityObject;
import de.isuret.polos.AetherOnePi.enums.AetherOnePins;
import de.isuret.polos.AetherOnePi.hotbits.IHotbitsClient;
import de.isuret.polos.AetherOnePi.processing.config.AetherOnePiProcessingConfiguration;
import de.isuret.polos.AetherOnePi.processing.config.Settings;
import de.isuret.polos.AetherOnePi.processing2.elements.AnalyseScreen;
import de.isuret.polos.AetherOnePi.processing2.elements.SettingsScreen;
import de.isuret.polos.AetherOnePi.utils.RateUtils;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AnalysisService {

    public static final int MAX_RATELIST_SIZE = 5000;

    @Setter
    @Autowired
    private IHotbitsClient hotbitsClient;

    @Setter
    @Autowired
    private PiService piService;

    @Setter
    private Integer maxValue = 100;

    public AnalysisResult analyseRateList(Iterable<Rate> rates) {

        AnalysisResult analysisResult = new AnalysisResult();
        Settings settings = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.SETTINGS);

        if (settings.getBoolean(SettingsScreen.ANALYSIS_VERY_HIGH_MAX_HIT, false)) {
            maxValue = 1000;
        } else {
            maxValue = 100;
        }

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

                    if (biggestLevel >= maxValue) {
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

            AnalysisResult sortedResult = analysisResult.sort().shorten(AnalyseScreen.MAX_ENTRIES);

            // now check the level, from physical to spiritual, 1 to 12
            for (RateObject rateObject : sortedResult.getRateObjects()) {

                Map<Integer,Integer> levels = new HashMap<>();

                for (int i=1; i<13; i++) {
                    levels.put(i,0);
                }

                for (int x=0; x<100; x++) {

                    Integer level = hotbitsClient.getInteger(1,12);
                    Integer value = levels.get(level);

                    if (hotbitsClient.getBoolean()) {
                        value += 1;
                        levels.put(level, value);
                    }
                }

                Integer maxValue = 0;

                for (Integer level : levels.keySet()) {

                    Integer value = levels.get(level);

                    if (value > maxValue) {
                        maxValue = value;
                        rateObject.setLevel(level);
                    }
                }
            }

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
            Integer value = vitalityMap.get(key).intValue() + 1;
            
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

    public AnalysisResult checkGeneralVitalityForAnalysis(AnalysisResult analysisResult) {

        analysisResult.setGeneralVitality(checkGeneralVitality());

        for (RateObject rateObject : analysisResult.getRateObjects()) {
            rateObject.setGv(checkGeneralVitality());
        }

        return analysisResult;
    }

    public String selectTrainingRate(List<Rate> rates) {
        int x = hotbitsClient.getInteger(0, rates.size() - 1);
        Rate rate = rates.get(x);
        return rate.getName();
    }
}
