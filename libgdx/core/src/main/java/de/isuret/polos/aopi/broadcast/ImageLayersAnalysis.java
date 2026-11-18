package de.isuret.polos.aopi.broadcast;

import java.util.List;
import java.util.Random;

/** Port of ImageLayersAnalysis.drawBroadcast: base first, each overlay with its original 5% chance. */
public class ImageLayersAnalysis {

    private final List<BroadcastImage> layers;

    public ImageLayersAnalysis(List<BroadcastImage> layers) {
        this.layers = List.copyOf(layers);
    }

    public void drawBroadcast(BroadcastContext target, float x, float y, float width, float height, Random random) {
        target.pushStyle();
        try {
            target.blendMode(BroadcastContext.BLEND);
            target.noTint();
            if (!layers.isEmpty()) {
                target.image(layers.getFirst(), x, y, width, height);
            }
            for (int index = 1; index < layers.size(); index++) {
                if (random.nextInt(1000) >= 950) {
                    target.image(layers.get(index), x, y, width, height);
                }
            }
        } finally {
            target.popStyle();
        }
    }
}
