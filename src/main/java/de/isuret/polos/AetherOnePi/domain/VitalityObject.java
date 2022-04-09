package de.isuret.polos.AetherOnePi.domain;

public class VitalityObject {

    private Integer number;
    private Integer value;

    public VitalityObject(Integer number, Integer value) {
        this.number = number;
        this.value = value;
    }

    public VitalityObject() {}

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
