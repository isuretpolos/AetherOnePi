package de.isuret.polos.AetherOnePi.imagelayers;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import processing.core.PImage;
import processing.core.PApplet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ImageLayersAnalysis contains an analysis of an image or layers of images
 */
public class ImageLayersAnalysis {

    private AetherOneUI p;
    private final String baseImageName;

    public List<ImageLayer> imageLayers = new ArrayList<>();
    private int x = 0;
    private int y = 73;
    private static int RESIZE_HEIGHT = 460;
    private int min = 256;
    private int max = 0;
    private float relativeGVratio = 0f;
    private float relativeMax = 0f;

    public ImageLayersAnalysis(AetherOneUI p, File directory) {

        this.p = p;
        this.baseImageName = directory.getName() + ".png";
        this.x = p.getGuiElements().getBorder() + 5;

        if (directory.getName().startsWith("IMAGE")) {
            init(directory);
        }
    }

    private void init(File directory) {
        for (File file:directory.listFiles()) {

            if (file.isDirectory()) continue;
            if (!file.isFile()) continue;
            if (!file.getName().equals(baseImageName)) continue;
            if (!file.getName().toLowerCase().endsWith("png")) continue;

            PImage image = p.loadImage(file.getAbsolutePath());
            image.resize(0, RESIZE_HEIGHT);
            imageLayers.add(new ImageLayer(image, file.getName()));
        }

        for (File file:directory.listFiles()) {

            if (file.isDirectory()) continue;
            if (!file.isFile()) continue;
            if (file.getName().equals(baseImageName)) continue;
            if (!file.getName().toLowerCase().endsWith("png")) continue;

            PImage image = p.loadImage(file.getAbsolutePath());
            image.resize(0, RESIZE_HEIGHT);
            imageLayers.add(new ImageLayer(image, file.getName()));
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

                        if (p.getHotbitsClient().getInteger(1,6) == 6) {
                            gv++;
                        }
                    }
                }

                if (gv > max) max = gv;
                if (gv < min) min = gv;
                imageLayer.setGv(gv);
            }
        }

        float minF = (float) min;
        float maxF = (float) max;
        relativeGVratio = 256 / (maxF - minF);
        relativeMax = (float)(max - min);

        System.out.println("min " + min + " and max " + max + " after " + rounds + " rounds with an relative GV ratio of " + relativeGVratio);

        for (ImageLayer imageLayer : imageLayers) {
            float relativeGV = relativeGVratio * (imageLayer.getGv() - min);
            System.out.println(imageLayer.getName() + " relative GV " + relativeGV);
        }
    }

    public void draw() {

        int layer = 0;

        for (ImageLayer imageLayer : imageLayers) {

            if (layer == 0) {
                p.image(imageLayer.getImage(), x,y);
            } else {
                // add transparency
                float relativeGV = relativeGVratio * (imageLayer.getGv() - min);
                p.tint(255, relativeGV);
                p.image(imageLayer.getImage(), x, y);
            }

            p.noTint();
            layer++;
        }
    }

    /** Always paint the collection's base, then randomly paint individual overlays on top. */
    public void drawBroadcast(PApplet target, float x, float y, float width, float height, Random random) {
        target.pushStyle();
        try {
            target.blendMode(PApplet.BLEND);
            target.imageMode(PApplet.CORNER);
            target.noTint();
            for (ImageLayer layer : imageLayers) {
                if (layer.getName().equals(baseImageName)) {
                    target.image(layer.getImage(), x, y, width, height);
                    break;
                }
            }
            for (ImageLayer layer : imageLayers) {
                if (!layer.getName().equals(baseImageName) && random.nextInt(1000) >= 950) {
                    target.image(layer.getImage(), x, y, width, height);
                }
            }
        } finally {
            target.popStyle();
        }
    }

    public AetherOneUI getP() {
        return p;
    }

    public void setP(AetherOneUI p) {
        this.p = p;
    }

    public List<ImageLayer> getImageLayers() {
        return imageLayers;
    }

    public void setImageLayers(List<ImageLayer> imageLayers) {
        this.imageLayers = imageLayers;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public static int getResizeHeight() {
        return RESIZE_HEIGHT;
    }

    public static void setResizeHeight(int resizeHeight) {
        RESIZE_HEIGHT = resizeHeight;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public float getRelativeGVratio() {
        return relativeGVratio;
    }

    public void setRelativeGVratio(float relativeGVratio) {
        this.relativeGVratio = relativeGVratio;
    }

    public float getRelativeMax() {
        return relativeMax;
    }

    public void setRelativeMax(float relativeMax) {
        this.relativeMax = relativeMax;
    }
}
