package de.isuret.polos.AetherOnePi.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RateShortenerUtil {

    public static void main(String [] args) throws IOException {
        File rateList = new File(args[0]);
        File enhancedRateList = new File(args[1]);

        List<String> rates = FileUtils.readLines(rateList, "UTF-8");
        List<String> newRates = new ArrayList<>();

        for (String rate : rates) {
            if (rate.contains("-")) {
                String parts[] = rate.split("-");
                String newRate = RateUtils.cleanRateName(parts[0]);

                if (newRate.contains(" see ")) continue;

                if (newRate.length() > 0) {
                    newRates.add(newRate);
                }
            }
        }

        FileUtils.writeLines(enhancedRateList, "UTF-8", newRates);
    }
}
