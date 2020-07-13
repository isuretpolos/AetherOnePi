package de.isuret.polos.AetherOnePi.domain;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.elements.AnalyseScreen;
import de.isuret.polos.AetherOnePi.service.DataService;
import de.isuret.polos.AetherOnePi.utils.RateUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.*;

@Data
@NoArgsConstructor
public class StickPad {

    private DataService dataService = new DataService();
    private List<StickPadPosition> positions = new ArrayList<>();
    private Boolean generalVitalityChecking = false;

    public void addStickPadPosition(long x, long y) {
        positions.add(new StickPadPosition(x, y));
    }

    private class StickPadPosition {
        Long x;
        Long y;

        public StickPadPosition(long x, long y) {
            // interweave time and space
            Long timeInMillis = Calendar.getInstance().getTimeInMillis();
            this.x = x + timeInMillis;
            this.y = y + timeInMillis;
        }
    }

    public AnalysisResult analyze(String databaseName) {

        AnalysisResult analysisResult = new AnalysisResult();
        Map<String, Integer> ratesValues = new HashMap<>();

        try {
            dataService.refreshDatabaseList();
            List<Rate> rates = dataService.findAllBySourceName(databaseName);

            for (StickPadPosition stickPadPosition : positions) {
                Random randomRate = new Random(stickPadPosition.x);
                Integer randomRatePosition = randomRate.nextInt(rates.size());
                Rate rate = rates.get(randomRatePosition);

                Random randomValue = new Random(stickPadPosition.y);
                Integer randomEnergeticValue = randomValue.nextInt(10) + 1;

                if (ratesValues.get(rate.getName()) == null) {
                    ratesValues.put(rate.getName(), randomEnergeticValue);
                } else {
                    Integer energeticValue = ratesValues.get(rate.getName()) + randomEnergeticValue;
                    ratesValues.put(rate.getName(), energeticValue);
                }
            }

            positions.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String rate : ratesValues.keySet()) {
            RateUtils.insertRate(analysisResult, ratesValues, rate);
        }

        return analysisResult.sort();
    }

    public void checkGeneralVitality(AetherOneUI p) {

        List<Integer> list = new ArrayList<Integer>();

        for (StickPadPosition stickPadPosition : positions) {
            Random randomValue = new Random(stickPadPosition.x);
            Integer randomEnergeticValue = randomValue.nextInt(1000) + 1;
            list.add(randomEnergeticValue);
        }

        Collections.sort(list, Collections.reverseOrder());

        Integer gv = list.get(0);

        if (gv > 950) {

            int randomDice = new Random(positions.remove(0).y).nextInt(100);

            while (randomDice >= 50 && positions.size() > 0) {
                gv += randomDice;
                randomDice = new Random(positions.remove(0).y).nextInt(100);
            }
        }

        if (p.getGvCounter() == 0) {
            p.setGeneralVitality(gv);
            positions.clear();
            p.setGvCounter(p.getGvCounter() + 1);
            return;
        } else if (p.getGvCounter() > AnalyseScreen.MAX_ENTRIES) {
            p.setStickPadMode(false);
            setGeneralVitalityChecking(false);
            p.setStickPadGeneralVitalityMode(false);
            p.setGvCounter(0);
        } else {
            p.getAetherOneEventHandler().setRateGeneralVitality(gv);
        }

        p.setGvCounter(p.getGvCounter() + 1);
        positions.clear();
    }
}
