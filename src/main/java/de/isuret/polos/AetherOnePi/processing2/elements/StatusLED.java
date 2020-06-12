package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import lombok.Setter;

public class StatusLED implements IDrawableElement {

    private AetherOneUI p;
    private String tabName;
    private String text;
    @Setter
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
}
