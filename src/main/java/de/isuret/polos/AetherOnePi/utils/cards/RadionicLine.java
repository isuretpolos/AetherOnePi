package de.isuret.polos.AetherOnePi.utils.cards;

import java.awt.*;

public class RadionicLine {

    private int generalVitality = 0;
    private double degree = 0;
    private String signature;
    private Color color;

    public RadionicLine(int generalVitality, double degree, String signature, Color color) {
        this.generalVitality = generalVitality;
        this.degree = degree;
        this.signature = signature;
        this.color = color;
    }

    public RadionicLine() {}

    public int getGeneralVitality() {
        return generalVitality;
    }

    public void setGeneralVitality(int generalVitality) {
        this.generalVitality = generalVitality;
    }

    public double getDegree() {
        return degree;
    }

    public void setDegree(double degree) {
        this.degree = degree;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
