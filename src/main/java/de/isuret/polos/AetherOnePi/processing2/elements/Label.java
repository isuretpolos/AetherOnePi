package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

public class Label implements IDrawableElement {

    private AetherOneUI p;
    private String tabName;
    private String text;
    private Float x;
    private Float y;

    public Label(AetherOneUI p, String tabName, String text, Float x, Float y) {
        this.p = p;
        this.tabName = tabName;
        this.text = text;
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw() {
        p.noStroke();
        p.fill(255);
        p.textFont(p.getGuiElements().getFonts().get("default"), 14);
        p.text(text, x, y);
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return tabName;
    }
}
