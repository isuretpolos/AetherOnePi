package de.isuret.polos.AetherOnePi.domain;

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

    public Integer getOccurrence() {
        return occurrence;
    }

    public void setOccurrence(Integer occurrence) {
        this.occurrence = occurrence;
    }

    public Integer getOverallEnergeticValue() {
        return overallEnergeticValue;
    }

    public void setOverallEnergeticValue(Integer overallEnergeticValue) {
        this.overallEnergeticValue = overallEnergeticValue;
    }

    public Integer getOverallGV() {
        return overallGV;
    }

    public void setOverallGV(Integer overallGV) {
        this.overallGV = overallGV;
    }

    public RateObject getRateObject() {
        return rateObject;
    }

    public void setRateObject(RateObject rateObject) {
        this.rateObject = rateObject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
