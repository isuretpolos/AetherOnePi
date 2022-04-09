package de.isuret.polos.AetherOnePi.domain;

public class AnalysisData {

    private String intention;
    private String rateList;

    public AnalysisData() {}

    public String getIntention() {
        return intention;
    }

    public void setIntention(String intention) {
        this.intention = intention;
    }

    public String getRateList() {
        return rateList;
    }

    public void setRateList(String rateList) {
        this.rateList = rateList;
    }

    public AnalysisData(String intention, String rateList) {
        this.intention = intention;
        this.rateList = rateList;
    }
}
