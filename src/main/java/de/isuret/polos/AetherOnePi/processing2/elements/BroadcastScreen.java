package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;

public class BroadcastScreen implements IDrawableElement, MouseClickObserver {

    private AetherOneUI p;

    public BroadcastScreen(AetherOneUI p) {
        this.p = p;
    }

    @Override
    public void draw() {
//        p.noStroke();
//        p.fill(198, 220, 255, 50f);
//        p.rect(40,120,1205,410);
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return "BROADCAST";
    }

    @Override
    public void mouseClicked() {

    }
}
