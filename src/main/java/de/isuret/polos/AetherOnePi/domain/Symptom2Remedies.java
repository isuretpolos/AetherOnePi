package de.isuret.polos.AetherOnePi.domain;

import java.util.ArrayList;
import java.util.List;

public class Symptom2Remedies {

    private String symptom;
    private List<String> remedies = new ArrayList<>();

    public String getSymptom() {
        return symptom;
    }

    public void setSymptom(String symptom) {
        this.symptom = symptom;
    }

    public List<String> getRemedies() {
        return remedies;
    }

    public void setRemedies(List<String> remedies) {
        this.remedies = remedies;
    }
}
