package de.isuret.polos.AetherOnePi.utils;

public class WonderfulPlantsRemedy {

    private String name;
    private String series;
    private String phase;
    private String stage;

    public static WonderfulPlantsRemedy builder() {
        return new WonderfulPlantsRemedy();
    }

    public String getKey() {
        return series + "." + phase + "."  + stage + " " + name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WonderfulPlantsRemedy name(String name) {
        setName(name);
        return this;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }


    public WonderfulPlantsRemedy series(String series) {
        setSeries(series);
        return this;
    }

    public WonderfulPlantsRemedy phase(String phase) {
        setPhase(phase);
        return this;
    }

    public WonderfulPlantsRemedy stage(String stage) {
        setStage(stage);
        return this;
    }

    public WonderfulPlantsRemedy build() {
        return this;
    }
}
