package de.isuret.polos.AetherOnePi.domain;

public class Potency {

    private String potencyType;
    private Integer potencyStrength;

    public String getPotencyType() {
        return potencyType;
    }

    public void setPotencyType(String potencyType) {
        this.potencyType = potencyType;
    }

    public Integer getPotencyStrength() {
        return potencyStrength;
    }

    public void setPotencyStrength(Integer potencyStrength) {
        this.potencyStrength = potencyStrength;
    }

    @Override
    public String toString() {
        return potencyType + " " + potencyStrength;
    }
}
