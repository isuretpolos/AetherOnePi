package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

public class StatusLED implements IDrawableElement {

    private AetherOneUI p;
    private String tabName;
    private String text;
    private boolean on = false;
    private float x;
    private float y;

    public StatusLED(AetherOneUI p, String tabName, String text, float x, float y) {
        this.p = p;
        this.tabName = tabName;
        this.text = text;
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw() {

        p.noStroke();

        if (on) {
            p.fill(0,240,0);
        } else {
            p.fill(80);
        }

        p.rect(x,y,20,10);

        p.fill(255);
        p.textFont(p.getGuiElements().getFonts().get("default"),14);
        p.text(text,x +24,y + 10);
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return tabName;
    }

    public AetherOneUI getP() {
        return p;
    }

    public void setP(AetherOneUI p) {
        this.p = p;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        this.on = on;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }
}
