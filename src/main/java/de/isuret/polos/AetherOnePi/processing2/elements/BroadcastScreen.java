package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;

public class BroadcastScreen implements IDrawableElement {

    private AetherOneUI p;

    public BroadcastScreen(AetherOneUI p) {
        this.p = p;
    }

    @Override
    public void draw() {

    }

    @Override
    public String getAssignedTabName() {
        return "BROADCAST";
    }
}
