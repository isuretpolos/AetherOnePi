package de.isuret.polos.AetherOnePi.domain;

import lombok.Data;

@Data
public class Potency {

    private String potencyType;
    private Integer potencyStrength;

    @Override
    public String toString() {
        return potencyType + " " + potencyStrength;
    }
}
