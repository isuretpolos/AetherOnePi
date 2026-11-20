package de.isuret.polos.aopi.broadcast;

import java.util.List;
import java.util.Random;

/** Each rate layer gets a hotbits-seeded generator for dynamic transparency without per-frame hotbits draws. */
public class ImageLayersAnalysis {

    private final List<BroadcastImage> layers;
    private final java.util.Map<String, Random[]> generatorsByBroadcast = new java.util.HashMap<>();

    public ImageLayersAnalysis(List<BroadcastImage> layers) {
        this.layers = List.copyOf(layers);
    }

    public void drawBroadcast(BroadcastContext target, float x, float y, float width, float height, Random unusedRandom) {
        if (layers.isEmpty()) { return; }
        Random[] generators = generatorsByBroadcast.computeIfAbsent(target.broadcastId(), ignored -> {
            Random[] evaluated = new Random[layers.size()];
            for (int index = 0; index < layers.size(); index++) {
                // One hotbits-backed nextInt(Integer.MAX_VALUE) supplies this layer's seed.
                // All subsequent animation draws use only this independent generator.
                evaluated[index] = new Random(target.getHotbitsClient().getInteger(Integer.MAX_VALUE - 1));
            }
            return evaluated;
        });
        target.pushStyle();
        try {
            target.blendMode(BroadcastContext.BLEND);
            target.noTint();
            for (int index = 0; index < layers.size(); index++) {
                // The Random argument remains only for compatibility with BroadcastElement's original API.
                int transparency = 100;
                if (generators[index].nextInt(1000) >= 999) {
                    transparency = generators[index].nextInt(101);
                }
                target.image(layers.get(index), x, y, width, height, (100 - transparency) / 100f);
            }
        } finally {
            target.popStyle();
        }
    }

    public void retainBroadcasts(java.util.Set<String> activeIds) {
        generatorsByBroadcast.keySet().retainAll(activeIds);
    }

    public boolean isEmpty() { return layers.isEmpty(); }
}
