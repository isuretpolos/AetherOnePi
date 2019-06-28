package de.isuret.polos.AetherOnePi.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class JanScholtenUtils {

    public static void main(String[] args) throws Exception {
        Reader in = new FileReader("src/main/resources/rates/JanScholtenWonderfulPlants_List.csv");

        Iterable<CSVRecord> records = CSVFormat.EXCEL.withDelimiter(';').parse(in);

        Map<String, WonderfulPlantsRemedy> remedies = new HashMap<>();
        Map<String, WonderfulPlantsFamily> families = new HashMap<>();

        Kingdoms kingdoms = new Kingdoms();

        for (CSVRecord record : records) {
            try {
                WonderfulPlantsRemedy remedy = WonderfulPlantsRemedy.builder().name(record.get(0)).series(record.get(1)).phase(record.get(2)).stage(record.get(3)).build();

                remedies.put(remedy.getKey(), remedy);

            } catch (Exception e) {
                try {
                    WonderfulPlantsFamily family = WonderfulPlantsFamily.builder().name(record.get(0)).series(record.get(1)).phase(record.get(2)).build().init();

                    if (families.containsKey(family.getKey())) {
                        families.get(family.getKey()).getAlternativeNames().add(family.getName());
                    } else {
                        families.put(family.getKey(), family);
                    }
                } catch (Exception e2) {

                    WonderfulPlantsFamily family = WonderfulPlantsFamily.builder().name(record.get(0)).series(record.get(1)).build().init();

                    if (families.containsKey(family.getKey())) {
                        families.get(family.getKey()).getAlternativeNames().add(family.getName());
                    } else {
                        families.put(family.getKey(), family);
                    }
                }
            }
        }

        in.close();


        for (WonderfulPlantsFamily family : families.values()) {
            for (WonderfulPlantsRemedy remedy : remedies.values()) {

                if (remedy.getSeries().equals(family.getSeries()) && remedy.getPhase().equals(family.getPhase())) {
                    family.getRemedies().add(remedy);
                }
            }

            if (family.getRemedies().isEmpty() && "00".equals(family.getPhase())) {
                kingdoms.getPlants().add(family);
            }

            Collections.sort(family.getRemedies(), new Comparator<WonderfulPlantsRemedy>() {
                @Override
                public int compare(WonderfulPlantsRemedy o1, WonderfulPlantsRemedy o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });

            System.out.println(family);
        }

        Collections.sort(kingdoms.getPlants(), new Comparator<WonderfulPlantsFamily>() {
            @Override
            public int compare(WonderfulPlantsFamily o1, WonderfulPlantsFamily o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        for (WonderfulPlantsFamily family : kingdoms.getPlants()) {

            for (int x = 1; x < 8; x++) {
                System.out.println(family.getKey() + " " + x + "0");

                WonderfulPlantsFamily subFamily = families.get(family.getSeries() + "." + x + "0");

                if (subFamily != null) {
                    family.getSubFamilies().add(subFamily);

                    for (int y = 1; y < 8; y++) {
                        System.out.println(x + "" + y);

                        WonderfulPlantsFamily innerSubFamily = families.get(family.getSeries() + "." + x + "" + y);

                        if (innerSubFamily != null) {
                            subFamily.getSubFamilies().add(innerSubFamily);
                        }
                    }
                }
            }
        }

        System.out.println("Mapping finished, now writing JSON file ...");

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File("target/kingdoms.json"), kingdoms);


        for (WonderfulPlantsFamily family : kingdoms.getPlants()) {

            for (WonderfulPlantsFamily innerFamily : family.getSubFamilies()) {
                for (WonderfulPlantsFamily subFamily : innerFamily.getSubFamilies()) {
                    StringBuilder str = new StringBuilder();
                    for (WonderfulPlantsRemedy remedy : subFamily.getRemedies()) {
                        str.append(remedy.getKey()).append("\n");
                    }

                    if (str.length() == 0) continue;

                    FileUtils.writeStringToFile(new File("target/" + subFamily.getKey() + "_" + subFamily.getName().replaceAll("ä","ae") + ".txt"), str.toString(), "UTF-8");
                }
            }
        }
    }

}
