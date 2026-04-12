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
                p.image(image, START_X, START_Y, WIDTH, HEIGHT);
                p.blend(image,0, 0, image.width, image.height, START_X * 2, START_Y * 2, WIDTH * 2, HEIGHT * 2, p.ADD);
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
