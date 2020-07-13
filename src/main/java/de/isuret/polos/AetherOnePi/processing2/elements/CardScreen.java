package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneConstants;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

public class CardScreen implements IDrawableElement {

    private AetherOneUI p;

    public CardScreen(AetherOneUI p) {
        this.p = p;
    }

    @Override
    public void draw() {
        p.fill(255);
        p.text("ANALYSIS", 35, 85);
        p.text("FOCUS PLATE", p.width / 2, 85);
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return AetherOneConstants.CARD;
    }
}
