package de.isuret.polos.AetherOnePi.service;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.RadionicScript;
import de.isuret.polos.AetherOnePi.domain.Rate;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RadionicsScriptService {

    private AetherOneUI p;

    public RadionicsScriptService(AetherOneUI p) {
        this.p = p;
    }

    public RadionicScript executeScript(String scriptName) {
        try {
            File file = p.getDataService().getScripts().get(scriptName + ".rscript");

            if (null == file || !file.exists()) {
                System.err.println("Script " + scriptName + " does not exist!");
                return null;
            }

            RadionicScript script = new RadionicScript(FileUtils.readLines(file, "UTF-8"));

            for (int i = 0; i < script.getLines().size(); ) {
                i = executeLine(script, script.getLines().get(i), i);
            }

            return script;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private int executeLine(RadionicScript script, String line, int pos) {

        if (line.startsWith("PRINT ")) {
            String output = line.substring("PRINT ".length()).trim();

            // check if variable
            if (script.getVariables().containsKey(output)) {
                Object value = script.getVariables().get(output);
                System.out.println(value);
                script.getOutput().add(value.toString());
            } else {
                System.out.println(output);
                script.getOutput().add(output);
            }

            return pos + 1;
        }

        // comments are ignored
        if (line.startsWith("## ")) {
            return pos + 1;
        }

        if (line.startsWith("GOTO ")) {
            String label = line.substring("GOTO ".length());
            Integer newPos = script.getLabelPosition(label);
            if (newPos != null) {
                System.out.println(line);
                return newPos;
            } else {
                String errMsg = "GOTO LABEL " + label + " DOES NOT EXIST";
                System.err.println(errMsg);
                script.getErrorMessages().add(errMsg);
                return pos + 1;
            }
        }

        // labels are ignored
        if (line.contains(":")) {
            return pos + 1;
        }

        // assigning variables
        if (line.contains("=")) {
            String [] parts = line.split("=");

            // Command that returns a value?
            // analyze
            if (parts[1].trim().startsWith("ANALYZE ")) {
                String databaseName = parts[1].trim().substring("ANALYZE ".length());
                script.getOutput().add("Analyzing " + databaseName);
                try {
                    List<Rate> rates = p.getDataService().findAllBySourceName(databaseName + ".txt");
                    AnalysisResult result = p.getAnalyseService().analyseRateList(rates);
                    result.setGeneralVitality(p.getAnalyseService().checkGeneralVitality());
                    for (RateObject rateObject : result.getRateObjects()) {
                        rateObject.setGv(p.getAnalyseService().checkGeneralVitality());
                    }
                    script.getVariables().put(parts[0].trim(), result); // <-- result of analysis goes into variable
                } catch (IOException e) {
                    e.printStackTrace();
                    script.getErrorMessages().add(e.getMessage());
                }
            } else {
                script.getVariables().put(parts[0].trim(), parts[1].trim());
            }

            return pos + 1;
        }

        script.getErrorMessages().add(line);
        System.err.println("NON INTERPRETED LINE >> " + line);
        return pos + 1;
    }
}
