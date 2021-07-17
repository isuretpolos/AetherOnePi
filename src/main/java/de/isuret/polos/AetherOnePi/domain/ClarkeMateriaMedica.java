package de.isuret.polos.AetherOnePi.domain;

import java.util.ArrayList;
import java.util.List;

public class ClarkeMateriaMedica {

    private List<Symptom2Remedies> clinicalSymptoms = new ArrayList<>();

    public List<Symptom2Remedies> getClinicalSymptoms() {
        return clinicalSymptoms;
    }

    public void setClinicalSymptoms(List<Symptom2Remedies> clinicalSymptoms) {
        this.clinicalSymptoms = clinicalSymptoms;
    }
}
