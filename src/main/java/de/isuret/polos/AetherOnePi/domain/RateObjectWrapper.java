package de.isuret.polos.AetherOnePi.domain;

import lombok.Data;

@Data
public class RateObjectWrapper {

    private Integer occurrence = 0;
    private Integer overallEnergeticValue = 0;
    private Integer overallGV = 0;
    private RateObject rateObject;
    private String name;

    public void addRate(RateObject rate) {
        this.name = rate.getNameOrRate();
        incrementOccurrence();
        addEnergeticValue(rate.getEnergeticValue());
        addGV(rate.getGv());
    }

    private void incrementOccurrence() {
        this.occurrence++;
    }

    private void addEnergeticValue(Integer energeticValue) {
        this.overallEnergeticValue += energeticValue;
    }

    private void addGV(Integer gv) {
        this.overallGV += gv;
    }
}
