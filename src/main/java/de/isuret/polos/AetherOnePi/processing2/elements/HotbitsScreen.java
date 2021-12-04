package de.isuret.polos.AetherOnePi.processing2.elements;

import com.github.sarxos.webcam.Webcam;
import de.isuret.polos.AetherOnePi.processing2.AetherOneConstants;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.events.MouseClickObserver;
import processing.core.PImage;

import java.awt.image.BufferedImage;

/**
 * HotbitsScreen handling TRNG (TrueRandomNumber Generator)
 */
public class HotbitsScreen implements IDrawableElement, MouseClickObserver {

    private AetherOneUI p;

    public HotbitsScreen(AetherOneUI p) {
        this.p = p;
    }

    @Override
    public void draw() {
        p.fill(255, 255, 255);
        int y = 120;
        int i = 0;

        p.text("1) CLICK ON 'SHOW WEBCAMS'", 60, y); y += 20;
        p.text("2) TYPE THE NUMBER OF THE WEBCAM YOU WANT TO USE IN THE TEXTFIELD", 60, y); y += 20;
        p.text("3) CLICK ON 'SET WEBCAM' (YOUR WEBCAM SHOULD BE HIGHLIGHTED)", 60, y); y += 20;
        p.text("4) CLICK ON 'GET HOTBITS' (NOW YOU SHOULD SEE A INCREASE IN THE PACKAGES)", 60, y); y += 20;
        y += 20;
        if (p.getWebcamList() == null) return;

        for (Webcam webcam : p.getWebcamList()) {
            p.fill(255, 255, 255);
            p.text(webcam.getName() + " [" + i + "]", 60, y);

            if (p.getWebCamNumber() != null && p.getWebCamNumber() == i) {
                p.text("*", 50, y);
                p.fill(0,255,255,50f);
                p.rect(45,y-15,300,20);
            }

            y += 20;
            i++;
        }

        if (p.getWebcam() != null) {
            p.fill(0, 255, 0);
            p.text("Packages " + p.getCountPackages() + " of 20000", 60, y);

            BufferedImage image = p.getWebcamImage();
            if (image != null && !p.isHotbitsFromWebCamAcquiring()) {
                p.image(new PImage(p.getWebcamImage()), 1000, 150);
            }

            if (p.isHotbitsFromWebCamAcquiring()) {
                p.fill(255, 100, 100); y += 20;
                p.text("ACQUIRING HOTBITS FROM WEBCAM (NO IMAGE IS SHOWN IN ORDER TO PRESERVE INDETERMITATED STATE)", 60, y);
                p.fill(0, 255, 0); y += 20;
                p.text("YOU CAN CONTINUE WORKING WHILE THIS PROCESS CONTINUES IN THE BACKGROUND!", 60, y);
            }
        }
    }

    @Override
    public void setDrawOrderByType(int i) {

    }

    @Override
    public String getAssignedTabName() {
        return AetherOneConstants.HOTBITS;
    }

    @Override
    public void mouseClicked() {

    }
}
