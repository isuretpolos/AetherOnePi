package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneConstants;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;

public class ImageLayerScreen implements IDrawableElement, MouseClickObserver  {

    private AetherOneUI p;

    public ImageLayerScreen(AetherOneUI p) {
        this.p = p;
    }

    @Override
    public void draw() {

        if (p.getImageLayersAnalysis() != null) {
            p.getImageLayersAnalysis().draw();
        }
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return AetherOneConstants.IMAGE;
    }

    @Override
    public void mouseClicked() {

    }
}
