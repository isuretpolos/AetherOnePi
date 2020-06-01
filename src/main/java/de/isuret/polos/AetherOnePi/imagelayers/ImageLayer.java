package de.isuret.polos.AetherOnePi.imagelayers;

import lombok.Data;
import processing.core.PImage;

@Data
public class ImageLayer {

    private String name;
    private Integer gv = 0;
    private PImage image;

    public ImageLayer(PImage image, String name) {
        this.image = image;
        this.name = name;
    }
}
