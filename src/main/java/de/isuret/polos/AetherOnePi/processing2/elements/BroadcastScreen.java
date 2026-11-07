package de.isuret.polos.AetherOnePi.processing2.elements;

import controlP5.Textfield;
import de.isuret.polos.AetherOnePi.imagelayers.ImageLayersAnalysis;
import de.isuret.polos.AetherOnePi.processing2.AetherOneConstants;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;

public class BroadcastScreen implements IDrawableElement, MouseClickObserver {

    private AetherOneUI p;

    public BroadcastScreen(AetherOneUI p) {
        this.p = p;
    }

    public static ImageLayersAnalysis getBroadcastImageLayers(AetherOneUI p) {
        if (!p.getSettings().getBoolean(SettingsScreen.BROADCAST_IMAGE_LAYERS, false)) {
            return null;
        }
        return p.getImageLayersAnalysis();
    }

    @Override
    public void draw() {
        p.fill(255);

        String signature = ((Textfield) p.getGuiElements().getCp5().get(AetherOneConstants.SIGNATURE)).getText();
        if (signature.length() > 0) {
            p.text("Rate: " + p.generateRate(signature), 900, 90);
        }
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
