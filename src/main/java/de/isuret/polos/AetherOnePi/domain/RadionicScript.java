package de.isuret.polos.AetherOnePi.domain;

import de.isuret.polos.AetherOnePi.exceptions.RadionicsScriptException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class RadionicScript {

    private List<String> errorMessages = new ArrayList<>();
    private List<String> output = new ArrayList<>();
    private Map<String, Object> variables = new HashMap<>();

    /**
     * Instruction lines to be interpreted
     */
    private List<String> lines = new ArrayList<>();

    /**
     * GOTO line labels
     */
    private Map<Integer,String> lineLabels = new HashMap<>();

    public RadionicScript(List<String> lines) {
        this.lines = lines;
        init();
    }

    public List<String> getOutput() {
        return output;
    }

    public void setOutput(List<String> output) {
        this.output = output;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }

    public Map<Integer, String> getLineLabels() {
        return lineLabels;
    }

    public void setLineLabels(Map<Integer, String> lineLabels) {
        this.lineLabels = lineLabels;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    private void init() {
        for (int i=0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains(":")) {
                String label = line.replace(":","").trim();
                if (lineLabels.containsValue(label)) throw new RadionicsScriptException("Label " + label + " already exist at line " + getLabelPosition(label));
                lineLabels.put(i, label);
            }
        }
    }

    public Integer getLabelPosition(String value) {
        AtomicReference<Integer> result = new AtomicReference<>();
        lineLabels.entrySet().forEach(integerStringEntry -> {
            if (integerStringEntry.getValue().equals(value)) {
                result.set(integerStringEntry.getKey());
            }
        });
        return result.get();
    }
}
