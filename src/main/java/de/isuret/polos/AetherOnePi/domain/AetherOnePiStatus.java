package de.isuret.polos.AetherOnePi.domain;

/**
 * A bean containing status data from the aetherOnePi server
 */
public class AetherOnePiStatus {

    private Boolean pseudoRandom = true;
    private Boolean broadcasting = false;
    private Boolean clearing = false;
    private Boolean grounding = false;
    private Boolean copying = false;
    private Integer hotbitsPackages = 0;
    private Integer progress = 0;
    private Integer queue = 0;
    private String text = "";

    public AetherOnePiStatus() {}

    public AetherOnePiStatus(Boolean pseudoRandom, Boolean broadcasting, Boolean clearing, Boolean grounding, Boolean copying, Integer hotbitsPackages, Integer progress, Integer queue, String text) {
        this.pseudoRandom = pseudoRandom;
        this.broadcasting = broadcasting;
        this.clearing = clearing;
        this.grounding = grounding;
        this.copying = copying;
        this.hotbitsPackages = hotbitsPackages;
        this.progress = progress;
        this.queue = queue;
        this.text = text;
    }

    public Boolean getPseudoRandom() {
        return pseudoRandom;
    }

    public void setPseudoRandom(Boolean pseudoRandom) {
        this.pseudoRandom = pseudoRandom;
    }

    public Boolean getBroadcasting() {
        return broadcasting;
    }

    public void setBroadcasting(Boolean broadcasting) {
        this.broadcasting = broadcasting;
    }

    public Boolean getClearing() {
        return clearing;
    }

    public void setClearing(Boolean clearing) {
        this.clearing = clearing;
    }

    public Boolean getGrounding() {
        return grounding;
    }

    public void setGrounding(Boolean grounding) {
        this.grounding = grounding;
    }

    public Boolean getCopying() {
        return copying;
    }

    public void setCopying(Boolean copying) {
        this.copying = copying;
    }

    public Integer getHotbitsPackages() {
        return hotbitsPackages;
    }

    public void setHotbitsPackages(Integer hotbitsPackages) {
        this.hotbitsPackages = hotbitsPackages;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Integer getQueue() {
        return queue;
    }

    public void setQueue(Integer queue) {
        this.queue = queue;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
