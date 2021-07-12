package de.isuret.polos.AetherOnePi.utils;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RateUtils {

    public static final String DIVIDER_A = " ----- ";

    private RateUtils() {}

    public static void insertRate(final AnalysisResult analysisResult, final Map<String, Integer> ratesValues, String rate) {
        String parts [] = rate.split("\t");

        if (parts.length > 1) {
            analysisResult.getRateObjects().add(new RateObject(ratesValues.get(rate), parts[0], parts[1], 0, 0, 0, 0, null,0));
        } else {
            analysisResult.getRateObjects().add(new RateObject(ratesValues.get(rate), rate, null, 0, 0, 0, 0, null,0));
        }
    }

    public static void main(String args[]) throws Exception {

        if (args.length == 0) {
            System.out.println("SYNTAX:\nRateUtils.main <rateFile> <newRateFile> <UrlPattern>\nUrlPattern example: http://materia-medica-url.org/#FIRST_LETTER#/#SHORTNAME#.htm");
            return;
        }

        File rateList = new File(args[0]);
        File enhancedRateList = new File(args[1]);

        List<String> rates = FileUtils.readLines(rateList, "UTF-8");
        List<String> newRates = new ArrayList<>();

        for (String rate : rates) {
            if (rate.contains(DIVIDER_A)) {
                String parts[] = rate.split(DIVIDER_A);
                String newRate = cleanRateName(parts[0]);
                String firstLetter = parts[parts.length-1].substring(0,1).toLowerCase();
                String shortName = parts[parts.length-1].replace(".","").replaceAll(" ","_").toLowerCase();
                String url = args[2].replace("#FIRST_LETTER#", firstLetter).replace("#SHORTNAME#",shortName);

                newRate += "\t" + url;

                if (newRate.length() > 0) {
                    newRates.add(newRate);
                }
            }
//            System.out.println(rate + "\t" + args[2].replace("#FIRST_LETTER#", "n").replace("#SHORTNAME#","nux_v"));
        }

        FileUtils.writeLines(enhancedRateList, "UTF-8", newRates);
    }

    public static String cleanRateName(String part) {

        return part
                .replaceAll("æ","ae")
                .replaceAll("Æ","Ae")
                .replaceAll("Œ","Oe");
    }
}
