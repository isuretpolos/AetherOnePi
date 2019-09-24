package de.isuret.polos.AetherOnePi.utils;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.domain.Session;
import de.isuret.polos.AetherOnePi.service.DataService;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StatisticsGenerator {

    private StatisticsGenerator() {}

    public static void main(String [] args) throws IOException {

        DataService dataService = new DataService();
        Case caseObject = dataService.loadCase(new File(args[0]));
        start(caseObject);
    }

    public static void start(Case caseObject) {

        System.out.println("========================");
        System.out.println("====   STATISTIC     ===");
        Map<String,RateObject> rates = new HashMap<>();

        for (Session session : caseObject.getSessionList()) {
            for (AnalysisResult analysisResult : session.getAnalysisResults()) {
                for (RateObject rateObject : analysisResult.getRateObjects()) {

                    RateObject rate = rates.get(rateObject.getNameOrRate());

                    if (rate != null) {
                        rateObject.setEnergeticValue(rateObject.getEnergeticValue() + 1);
                    } else {
                        rateObject.setEnergeticValue(1);
                    }

                    rates.put(rateObject.getNameOrRate(), rateObject);
                }
            }
        }

        List<RateObject> rateList = new ArrayList<>();
        rateList.addAll(rates.values());

        Collections.sort(rateList, new Comparator<RateObject>() {
            @Override
            public int compare(RateObject o1, RateObject o2) {
                return o2.getEnergeticValue().compareTo(o1.getEnergeticValue());
            }
        });

        int count = 0;

        for (RateObject rateObject : rateList) {
            System.out.println(rateObject.getNameOrRate() + " - " + rateObject.getEnergeticValue());
            count++;
            if (count > 10) {
                break;
            }
        }

        System.out.println("========================");
    }
}
