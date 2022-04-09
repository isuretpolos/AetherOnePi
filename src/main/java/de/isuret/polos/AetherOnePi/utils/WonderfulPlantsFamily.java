package de.isuret.polos.AetherOnePi.utils;

import java.util.ArrayList;
import java.util.List;

public class WonderfulPlantsFamily {

    private List<WonderfulPlantsFamily> subFamilies = new ArrayList<>();
    private List<WonderfulPlantsRemedy> remedies = new ArrayList<>();
    private List<String> alternativeNames = new ArrayList<>();

    private String name;
    private String series;
    private String phase;

    public static WonderfulPlantsFamily builder() {
        return new WonderfulPlantsFamily();
    }

    // Using builder it does not initialize the lists and @Singular does not work properly (so loose it, write your own stuff)
    public WonderfulPlantsFamily init() {
        alternativeNames = new ArrayList<>();
        subFamilies = new ArrayList<>();
        remedies = new ArrayList<>();
        return this;
    }

    public String getKey() {
        return series + "." + phase;
    }

    public List<WonderfulPlantsFamily> getSubFamilies() {
        return subFamilies;
    }

    public void setSubFamilies(List<WonderfulPlantsFamily> subFamilies) {
        this.subFamilies = subFamilies;
    }

    public List<WonderfulPlantsRemedy> getRemedies() {
        return remedies;
    }

    public void setRemedies(List<WonderfulPlantsRemedy> remedies) {
        this.remedies = remedies;
    }

    public List<String> getAlternativeNames() {
        return alternativeNames;
    }

    public void setAlternativeNames(List<String> alternativeNames) {
        this.alternativeNames = alternativeNames;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public WonderfulPlantsFamily name(String name) {
        setName(name);
        return this;
    }

    public WonderfulPlantsFamily series(String series) {
        setSeries(series);
        return this;
    }

    public WonderfulPlantsFamily phase(String phase) {
        setPhase(phase);
        return this;
    }

    public WonderfulPlantsFamily build() {
        return this;
    }
}
