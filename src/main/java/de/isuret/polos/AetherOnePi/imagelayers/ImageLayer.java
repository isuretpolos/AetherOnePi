package de.isuret.polos.AetherOnePi.imagelayers;

import lombok.Data;
import processing.core.PImage;

@Data
public class ImageLayer {

    private Integer gv = 0;
    private PImage image;

    public ImageLayer(PImage image) {
        this.image = image;
    }
}
