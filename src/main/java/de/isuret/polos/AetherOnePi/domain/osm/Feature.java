package de.isuret.polos.AetherOnePi.domain.osm;

import java.util.Calendar;

public class Feature {

    private String territoryName;
    private String simpleFeatureData;
    private String simpleFeatureType;
    private String note;
    private String url;
    private Calendar lastUpdate;

    public String getTerritoryName() {
        return territoryName;
    }

    public void setTerritoryName(String territoryName) {
        this.territoryName = territoryName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSimpleFeatureData() {
        return simpleFeatureData;
    }

    public void setSimpleFeatureData(String simpleFeatureData) {
        this.simpleFeatureData = simpleFeatureData;
    }

    public String getSimpleFeatureType() {
        return simpleFeatureType;
    }

    public void setSimpleFeatureType(String simpleFeatureType) {
        this.simpleFeatureType = simpleFeatureType;
    }

    public Calendar getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Calendar lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
