package de.isuret.polos.AetherOnePi.imagelayers;

import processing.core.PImage;

public class ImageLayer {

    private String name;
    private Integer gv = 0;
    private PImage image;

    public ImageLayer(PImage image, String name) {
        this.image = image;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGv() {
        return gv;
    }

    public void setGv(Integer gv) {
        this.gv = gv;
    }

    public PImage getImage() {
        return image;
    }

    public void setImage(PImage image) {
        this.image = image;
    }
}
