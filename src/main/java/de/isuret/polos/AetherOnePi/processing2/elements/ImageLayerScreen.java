package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneConstants;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;
import processing.core.PImage;

public class ImageLayerScreen implements IDrawableElement, MouseClickObserver  {

    private AetherOneUI p;
    private final int START_X = 36;
    private final int START_Y = 75;
    private final int WIDTH = 1215;
    private final int HEIGHT = 460;

    public ImageLayerScreen(AetherOneUI p) {
        this.p = p;
    }

    @Override
    public void draw() {

        if (p.getImageLayersAnalysis() != null) {
            p.getImageLayersAnalysis().draw();
        }

        int imageCount = 0;

        for (PImage image : p.getClipBoardImages()) {
            if (imageCount > 0) {
                p.blend(image,START_X, START_Y, WIDTH, HEIGHT, START_X, START_Y, WIDTH, HEIGHT, p.DIFFERENCE);
            } else {
                p.image(image, START_X, START_Y, WIDTH, HEIGHT);
            }
            imageCount++;
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
