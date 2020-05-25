package de.isuret.polos.AetherOnePi.imagelayers;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import lombok.Data;
import processing.core.PImage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ImageLayersAnalysis contains an analysis of an image or layers of images
 */
@Data
public class ImageLayersAnalysis {

    private AetherOneUI p;

    public List<ImageLayer> imageLayers = new ArrayList<>();
    private int x = 0;
    private int y = 73;
    private static int RESIZE_HEIGHT = 460;
    private int min = 256;
    private int max = 0;

    public ImageLayersAnalysis(AetherOneUI p, File directory) {

        this.p = p;
        this.x = p.getGuiElements().getBorder() + 5;

        if (directory.getName().startsWith("IMAGE")) {
            init(directory);
        }
    }

    private void init(File directory) {
        for (File file:directory.listFiles()) {

            if (file.isDirectory()) continue;
            if (!file.isFile()) continue;
            if (!file.getName().startsWith("IMAGE")) continue;

            PImage image = p.loadImage(file.getAbsolutePath());
            image.resize(0, RESIZE_HEIGHT);
            imageLayers.add(new ImageLayer(image));
        }

        for (File file:directory.listFiles()) {

            if (file.isDirectory()) continue;
            if (!file.isFile()) continue;
            if (file.getName().startsWith("IMAGE")) continue;

            PImage image = p.loadImage(file.getAbsolutePath());
            image.resize(0, RESIZE_HEIGHT);
            imageLayers.add(new ImageLayer(image));
            System.out.println(file.getName());
        }
    }

    public void analyze() {

        min = 256;
        max = 0;
        int rounds = 0;

        while (max < 162) {
            rounds++;
            for (ImageLayer imageLayer : imageLayers) {

                int gv = 0;

                for (int x = 0; x < 256; x++) {
                    if (p.getHotbitsClient().getInteger(0, 1000) >= 500) {
                        gv++;
                    }
                }

                if (gv > max) max = gv;
                if (gv < min) min = gv;
                imageLayer.setGv(gv);
            }
        }

        System.out.println("min " + min + " and max " + max + " after " + rounds + " rounds");
    }

    public void draw() {

        int layer = 0;

        for (ImageLayer imageLayer : imageLayers) {

            if (layer == 0) {
                p.image(imageLayer.getImage(), x,y);
            } else {
                // add transparency
                p.tint(255, imageLayer.getGv());
                p.image(imageLayer.getImage(), x, y);
            }

            p.noTint();
            layer++;
        }
    }
}
