package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneConstants;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

public class SessionScreen implements IDrawableElement {

    private AetherOneUI p;

    public SessionScreen(AetherOneUI p) {
        this.p = p;
    }

    @Override
    public void draw() {

        if (p.getEssentielQuestion() != null) {
            p.fill(255);
            p.text(p.getEssentielQuestion(), 50,150);
        }
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return AetherOneConstants.SESSION;
    }
}
