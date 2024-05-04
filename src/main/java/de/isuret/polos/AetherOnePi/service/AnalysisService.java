package de.isuret.polos.AetherOnePi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.isuret.polos.AetherOnePi.domain.*;
import de.isuret.polos.AetherOnePi.enums.AetherOnePins;
import de.isuret.polos.AetherOnePi.hotbits.IHotbitsClient;
import de.isuret.polos.AetherOnePi.processing2.elements.AnalyseScreen;
import de.isuret.polos.AetherOnePi.processing2.elements.SettingsScreen;
import de.isuret.polos.AetherOnePi.utils.AetherOnePiProcessingConfiguration;
import de.isuret.polos.AetherOnePi.utils.RateUtils;
import j2html.tags.ContainerTag;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static j2html.TagCreator.*;

public class AnalysisService {

    public static final int MAX_RATELIST_SIZE = 5000;
    private IHotbitsClient hotbitsClient;
    private PiService piService;
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
            if (max < 120) max = 120;
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
            int trials = 0;

            while (!analysisFinished) {
                for (String rate : ratesValues.keySet()) {

                    trials++;
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

            analysisResult.setNumberOfTrials(trials);

            if (piService != null) {
                piService.high(AetherOnePins.CONTROL);
            }

            for (String rate : ratesValues.keySet()) {
                RateUtils.insertRate(analysisResult, ratesValues, rate);
            }

            AnalysisResult sortedResult = analysisResult.sort().shorten(AnalyseScreen.MAX_ENTRIES_INTERNAL);

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
        List<Integer> list = new ArrayList<Integer>();

        for (int x = 0; x < 3; x++) {
            list.add(getHotbitsClient().getInteger(1000));
        }

        Collections.sort(list, Collections.reverseOrder());

        Integer gv = list.get(0);

        if (gv > 950) {
            int randomDice = getHotbitsClient().getInteger(100);

            while (randomDice >= 50) {
                gv += randomDice;
                randomDice = getHotbitsClient().getInteger(100);
            }
        }
        return gv;
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
        Map<String, String> remedy2url = new HashMap<>();
        TreeMap<String, Integer> symptomsCounter = new TreeMap<>();

        for (RateObject rateObject : analysisResult.getRateObjects()) {
            remedy2url.put(rateObject.getNameOrRate(), rateObject.getUrl());
        }

        for (Symptom2Remedies symptom2Remedies : clarkeMateriaMedica.getClinicalSymptoms()) {
            // collect the clinical symptoms for the one remedy
            if (symptom2Remedies.getRemedies().contains(nameOrRate)) {

                clinicalSymptoms.add(symptom2Remedies.getSymptom());
                // additionally collect the same symptom for the other remedies in the analysis
                for (RateObject rateObject : analysisResult.getRateObjects()) {
                    if (nameOrRate.equals(rateObject.getNameOrRate())) continue;
                    if (symptom2Remedies.getRemedies().contains(rateObject.getNameOrRate())) {

                        if (symptomsCounter.containsKey(symptom2Remedies.getSymptom())) {
                            Integer c = symptomsCounter.get(symptom2Remedies.getSymptom());
                            symptomsCounter.put(symptom2Remedies.getSymptom(), c + 1);
                        } else {
                            symptomsCounter.put(symptom2Remedies.getSymptom(), 1);
                        }

                        if (otherRemediesClinicalSymptoms.containsKey(rateObject.getNameOrRate())) {
                            otherRemediesClinicalSymptoms.get(rateObject.getNameOrRate()).add(symptom2Remedies);
                        } else {
                            List<Symptom2Remedies> symptoms = new ArrayList<>();
                            symptoms.add(symptom2Remedies);
                            otherRemediesClinicalSymptoms.put(rateObject.getNameOrRate(), symptoms);
                        }
                    }
                }
            }

            // and the symptoms of the other remedies that they do not have in common
            for (RateObject rateObject : analysisResult.getRateObjects()) {
                if (symptom2Remedies.getRemedies().contains(rateObject.getNameOrRate())) {

                    if (symptomsCounter.containsKey(symptom2Remedies.getSymptom())) {
                        Integer c = symptomsCounter.get(symptom2Remedies.getSymptom());
                        symptomsCounter.put(symptom2Remedies.getSymptom(), c + 1);
                    } else {
                        symptomsCounter.put(symptom2Remedies.getSymptom(), 1);
                    }

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

        List<SymptomCounter> symptomCounterList = new ArrayList<>();
        symptomsCounter.keySet().forEach( symptom -> symptomCounterList.add(new SymptomCounter(symptom, symptomsCounter.get(symptom))));
        symptomCounterList.sort((o1, o2) -> o2.getCounter().compareTo(o1.getCounter()));

        for (String remedy : otherRemediesClinicalSymptoms.keySet()) {
            if (!otherRemediesClinicalSymptoms2.containsKey(remedy)) {
                otherRemediesClinicalSymptoms2.put(remedy, new ArrayList<>());
            }
        }

        List<String> clinicalSymptomsInCommon = new ArrayList<>();


        String htmlString = html(
                head(
                        title("Clinical Materia Medica - " + nameOrRate),
                        link().withRel("stylesheet").withHref("https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css")
                ),
                body(
                        div(attrs(".container"),
                                h1( a().withHref(remedy2url.get(nameOrRate)).withText("== " + nameOrRate + " ==") ),
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
                                                h4(a().withHref(remedy2url.get(remedy)).withText(remedy)),
                                                div(otherRemediesClinicalSymptoms.get(remedy).stream().map(
                                                        symptom2Remedies -> span(attrs(".badge .bg-success"), text(symptom2Remedies.getSymptom()))
                                                ).toArray(ContainerTag[]::new)),
                                                div(otherRemediesClinicalSymptoms2.get(remedy).stream().map(
                                                        symptom2Remedies -> span(attrs(".badge .bg-dark"), text(symptom2Remedies.getSymptom()))
                                                ).toArray(ContainerTag[]::new))
                                        )
                                ).toArray(ContainerTag[]::new)),
                                table(attrs(".table .table-striped .table-hover")).with(
                                        thead(attrs(".table-dark")).with(
                                            tr().with(
                                                th().with(span("COUNTER")),
                                                th().with(span("SYMPTOM")))),
                                        tbody().with(
                                                symptomCounterList.stream().limit(20).map(s ->
                                                        tr().with(
                                                                td().with(span(String.valueOf(s.getCounter()))),
                                                                td().with(span(s.getName()))))))
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

    public IHotbitsClient getHotbitsClient() {
        return hotbitsClient;
    }

    public void setHotbitsClient(IHotbitsClient hotbitsClient) {
        this.hotbitsClient = hotbitsClient;
    }

    public PiService getPiService() {
        return piService;
    }

    public void setPiService(PiService piService) {
        this.piService = piService;
    }

    public Integer getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Integer maxValue) {
        this.maxValue = maxValue;
    }

    public ClarkeMateriaMedica getClarkeMateriaMedica() {
        return clarkeMateriaMedica;
    }

    public void setClarkeMateriaMedica(ClarkeMateriaMedica clarkeMateriaMedica) {
        this.clarkeMateriaMedica = clarkeMateriaMedica;
    }

    public List<Integer> searchAnomaly(Integer width, Integer height) {

        List<Integer> analysis = new ArrayList<>();

        for (int w=0; w < width; w++) {
            for (int h=0; h < height; h++) {
                analysis.add(checkGeneralVitality());
            }
        }

        return analysis;
    }

    private class SymptomCounter {
        private String name;
        private Integer counter;

        public SymptomCounter(String name, Integer counter) {
            this.name = name;
            this.counter = counter;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getCounter() {
            return counter;
        }

        public void setCounter(Integer counter) {
            this.counter = counter;
        }
    }
}
