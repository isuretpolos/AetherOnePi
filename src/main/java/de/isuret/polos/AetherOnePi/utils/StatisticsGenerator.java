package de.isuret.polos.AetherOnePi.utils;

import de.isuret.polos.AetherOnePi.domain.Case;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.domain.RateObjectWrapper;
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

    public static void start(final Case caseObject) {

        System.out.println("========================");
        System.out.println("====   STATISTIC     ===");
        Map<String, RateObjectWrapper> rates = new HashMap<>();

        for (Session session : caseObject.getSessionList()) {
            if (session.getAnalysisResult() != null) {
                for (RateObject rateObject : session.getAnalysisResult().getRateObjects()) {

                    RateObjectWrapper rate = rates.get(rateObject.getNameOrRate());

                    if (rate == null) {
                        rate = new RateObjectWrapper();
                    }

                    rate.addRate(rateObject);
                    rates.put(rateObject.getNameOrRate(), rate);
                }
            }
        }

        List<RateObjectWrapper> rateList = new ArrayList<>();
        rateList.addAll(rates.values());

        Collections.sort(rateList, new Comparator<RateObjectWrapper>() {

            @Override
            public int compare(RateObjectWrapper o1, RateObjectWrapper o2) {
                int compareResult = o2.getOccurrence().compareTo(o1.getOccurrence());
                if (compareResult != 0) return compareResult;
                compareResult = o2.getOverallGV().compareTo(o1.getOverallGV());
                if (compareResult != 0) return compareResult;
                compareResult = o2.getOverallEnergeticValue().compareTo(o1.getOverallEnergeticValue());
                if (compareResult != 0) return compareResult;
                return o1.getName().compareTo(o2.getName());
            }
        });

        int count = 0;
        caseObject.getTopTenList().clear();

        for (RateObjectWrapper rate : rateList) {
            System.out.println(rate.getOccurrence() + " - " + rate.getOverallGV() + " " + rate.getOverallEnergeticValue() + " " + rate.getName());
            count++;

            caseObject.getTopTenList().add(rate);

            if (count > 10) {
                break;
            }
        }

        System.out.println("========================");
    }
}
