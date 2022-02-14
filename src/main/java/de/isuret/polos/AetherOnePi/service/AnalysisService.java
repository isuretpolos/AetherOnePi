package de.isuret.polos.AetherOnePi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.domain.*;
import de.isuret.polos.AetherOnePi.enums.AetherOnePins;
import de.isuret.polos.AetherOnePi.hotbits.IHotbitsClient;
import de.isuret.polos.AetherOnePi.processing.config.AetherOnePiProcessingConfiguration;
import de.isuret.polos.AetherOnePi.processing.config.Settings;
import de.isuret.polos.AetherOnePi.processing2.elements.AnalyseScreen;
import de.isuret.polos.AetherOnePi.processing2.elements.SettingsScreen;
import de.isuret.polos.AetherOnePi.utils.RateUtils;
import j2html.tags.ContainerTag;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static j2html.TagCreator.*;

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

    private ClarkeMateriaMedica clarkeMateriaMedica;

    public AnalysisService() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            clarkeMateriaMedica = objectMapper.readValue(new File("data/radionics/HOMEOPATHY/clarkeMateriaMedica.json"), ClarkeMateriaMedica.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

                rateObject.setPotency(analyzePotency());
                Map<Integer, Integer> levels = new HashMap<>();

                for (int i = 1; i < 13; i++) {
                    levels.put(i, 0);
                }

                for (int x = 0; x < 100; x++) {

                    Integer level = hotbitsClient.getInteger(1, 12);
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
     *
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
        Map<Integer, Integer> vitalityMap = new HashMap<>();

        for (int x = 0; x < 101; x++) {

            vitalityMap.put(x, 0);
        }

        for (int x = 0; x < 3456; x++) {

            Integer key = hotbitsClient.getInteger(0, 100);
            Integer value = vitalityMap.get(key).intValue() + 1;

            vitalityMap.put(key, value);
        }

        List<VitalityObject> vitalityList = new ArrayList<>();

        for (int x = 0; x < 101; x++) {
            vitalityList.add(new VitalityObject(x, vitalityMap.get(x)));
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

    public String analyzePotency() {

        final String potencyType[] = {"D", "C", "LM", "Q", "FLUX"};
        final Integer potencyStrengthD[] = {0, 1, 3, 4, 6, 12, 30, 200};
        final Integer potencyStrengthC_OR_FLUX[] = {0, 1, 3, 6, 12, 30, 200, 1000, 10000, 50000, 100000, 1000000};
        List<Potency> potencies = new ArrayList<>();
        Map<Integer, Integer> potencyChoice = new HashMap<>();

        for (int x = 0; x < 100; x++) {
            Potency potency = new Potency();
            potency.setPotencyType(potencyType[hotbitsClient.getInteger(0, potencyType.length - 1)]);

            if (potency.getPotencyType().equals("D")) {
                potency.setPotencyStrength(potencyStrengthD[hotbitsClient.getInteger(0, potencyStrengthD.length - 1)]);
            } else if (potency.getPotencyType().equals("C") || potency.getPotencyType().equals("FLUX")) {
                potency.setPotencyStrength(potencyStrengthC_OR_FLUX[hotbitsClient.getInteger(0, potencyStrengthC_OR_FLUX.length - 1)]);
            } else {
                potency.setPotencyStrength(hotbitsClient.getInteger(1, 30));
            }

            potencyChoice.put(x, 0);
            potencies.add(potency);
        }

        while (true) {

            int pos = hotbitsClient.getInteger(0, 99);
            Integer strength = potencyChoice.get(pos);
            strength += 1;

            if (strength > 77) {
                return potencies.get(pos).toString();
            }

            potencyChoice.put(pos, strength);
        }
    }

    public File analyzeClarke(String nameOrRate, AnalysisResult analysisResult) {

        List<String> clinicalSymptoms = new ArrayList<>();
        Map<String, List<Symptom2Remedies>> otherRemediesClinicalSymptoms = new HashMap<>();
        Map<String, List<Symptom2Remedies>> otherRemediesClinicalSymptoms2 = new HashMap<>();

        for (Symptom2Remedies symptom2Remedies : clarkeMateriaMedica.getClinicalSymptoms()) {
            // collect the clinical symptoms for the one remedy
            if (symptom2Remedies.getRemedies().contains(nameOrRate)) {

                clinicalSymptoms.add(symptom2Remedies.getSymptom());
                // additionally collect the same symptom for the other remedies in the analysis
                for (RateObject rateObject : analysisResult.getRateObjects()) {
                    if (nameOrRate.equals(rateObject.getNameOrRate())) continue;
                    if (symptom2Remedies.getRemedies().contains(rateObject.getNameOrRate())) {
                        if (otherRemediesClinicalSymptoms.containsKey(rateObject.getNameOrRate())) {
                            otherRemediesClinicalSymptoms.get(rateObject.getNameOrRate()).add(symptom2Remedies);
                        } else {
                            List<Symptom2Remedies> symptoms = new ArrayList<>();
                            symptoms.add(symptom2Remedies);
                            otherRemediesClinicalSymptoms.put(rateObject.getNameOrRate(), symptoms);
                        }
                    }
                }
            } else {
                // and the symptoms of the other remedies that they do not have in common
                for (RateObject rateObject : analysisResult.getRateObjects()) {
                    if (symptom2Remedies.getRemedies().contains(rateObject.getNameOrRate())) {
                        if (otherRemediesClinicalSymptoms2.containsKey(rateObject.getNameOrRate())) {
                            otherRemediesClinicalSymptoms2.get(rateObject.getNameOrRate()).add(symptom2Remedies);
                        } else {
                            List<Symptom2Remedies> symptoms = new ArrayList<>();
                            symptoms.add(symptom2Remedies);
                            otherRemediesClinicalSymptoms2.put(rateObject.getNameOrRate(), symptoms);
                        }
                    }
                }
            }
        }

        for (String remedy : otherRemediesClinicalSymptoms.keySet()) {
            if (!otherRemediesClinicalSymptoms2.containsKey(remedy)) {
                otherRemediesClinicalSymptoms2.put(remedy, new ArrayList<>());
            }
        }

        List<String> clinicalSymptomsInCommon = new ArrayList<>();


        String htmlString = html(
                head(
                        title("Clinical Materia Medica - " + nameOrRate),
                        link().withRel("stylesheet").withHref("https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css")
                ),
                body(
                        div(attrs(".container"),
                                h1("== " + nameOrRate + " =="),
                                h3("Clinical symptoms"),
                                div(attrs(".alert .alert-secondary"),
                                        clinicalSymptoms.stream().map(
                                                symptom -> span(attrs(".badge .bg-success"), text(symptom))
                                        ).toArray(ContainerTag[]::new)
                                ),
                                h3("Compare"),
                                p("Other remedies that have the clinical symptoms in common:"),
                                div(otherRemediesClinicalSymptoms.keySet().stream().map(
                                        remedy -> div(attrs(".alert .alert-secondary"),
                                                h4(remedy),
                                                div(otherRemediesClinicalSymptoms.get(remedy).stream().map(
                                                        symptom2Remedies -> span(attrs(".badge .bg-success"), text(symptom2Remedies.getSymptom()))
                                                ).toArray(ContainerTag[]::new)),
                                                div(otherRemediesClinicalSymptoms2.get(remedy).stream().map(
                                                        symptom2Remedies -> span(attrs(".badge .bg-dark"), text(symptom2Remedies.getSymptom()))
                                                ).toArray(ContainerTag[]::new))
                                        )
                                ).toArray(ContainerTag[]::new))
                        )
                )
        ).renderFormatted();

        new File("logs").mkdir();
        File file = new File("logs/" + nameOrRate.replaceAll(" ", "") + ".html");
        try {
            FileUtils.writeStringToFile(file, htmlString, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
