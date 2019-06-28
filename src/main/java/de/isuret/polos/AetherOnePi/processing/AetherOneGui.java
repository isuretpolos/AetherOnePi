package de.isuret.polos.AetherOnePi.processing;

import controlP5.Textfield;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.hotbits.HotbitsClient;
import de.isuret.polos.AetherOnePi.processing.config.Settings;
import processing.core.PImage;

/**
 * The main drawing instance, outsourced from the Processing IDE
 */
public class AetherOneGui {

    private HotbitsClient hotbitsClient;
    private AetherOneProcessingMain p;
    private AetherOneCore core;
    private RadionicsElements radionicsElements;
    private Settings guiConf;

    public AetherOneGui(AetherOneProcessingMain pApplet, AetherOneCore core, RadionicsElements radionicsElements, HotbitsClient hotbitsClient) {
        this.p = pApplet;
        this.core = core;
        this.radionicsElements = radionicsElements;
        this.hotbitsClient = hotbitsClient;
        this.guiConf = p.getGuiConf();
    }

    public void draw() {
        // TODO move groups in own "movable" classes
        p.noTint();
        p.noStroke();
        p.background(0);
        p.image(p.getBackgroundImage(), 0, 0, p.width, p.height);
        p.fill(0, 150);
        paintBackgroundRect("main", 0, 0, 590, 720);
        paintBackgroundRect("photo", 590, 265, 360, 560);
        paintBackgroundRect("knobs12dials", 960, 265, 233, 720);
        p.fill(255);
        p.stroke(255);
        p.text("INPUT", 10, 25);
        p.text("OUTPUT", 10, 47);
        p.stroke(255);
        p.strokeWeight(1);
        p.fill(208, 147, 255);

        p.pushMatrix();
        p.translate(775, 155);
        p.rotate(p.frameCount * (float) 0.016);
        for (int x = 0; x < radionicsElements.nx; x++) {
            for (int y = 0; y < radionicsElements.ny; y++) {
                radionicsElements.d[x][y].display();
            }
        }
        p.popMatrix();

        //==================================================

        int x = 30;
        int y = 85; //90;
        drawGreenLED("PI CONNECTED", x, y, 20, !hotbitsClient.isPseudoRandomMode());
        drawBlueLED("CLEARING", x + 70, y, 20, p.getStatus().getClearing());
        drawGreenLED("ANALYSING", x + 140, y, 23, p.isAnalysing());
        drawGreenLED("BROADCASTING", x, y + 70, 20, p.getStatus().getBroadcasting());
        drawGreenLED("COPY", x + 70, y + 70, 10, p.getPiClient().isCopy());
        drawBlueLED("GROUNDING", x + 140, y + 70, 25, p.getStatus().getGrounding());
        drawRedLED("HOTBITS", x, y + 130, 20, hotbitsClient.isCollectingHotbits());
        drawBlueLED("SIMULATION", x + 70, y + 130, 26, hotbitsClient.isPseudoRandomMode());

        p.textSize(11);
        String trngModeText = "  TRNG Mode";
        if (p.isTrngMode() == false) {
            p.fill(255, 50, 10);
            trngModeText = "  PRNG Simulation Mode";
        }
        p.text("Hotbits " + core.getHotbits().size() + trngModeText + " " + p.getHotbitPackagesSizeText(), 10, 265);
        p.fill(255);

        p.textSize(13);//(16);
        p.stroke(0, 0, 255);

        p.fill(66, 244, 101);
        if (p.getStatusText() != null) {
            p.text(p.getStatusText(), 10, 310);
        }

        p.fill(164, 197, 249);
        if (p.getSelectedDatabase() != null) {
            p.text(p.getSelectedDatabase(), 10, 330);
        }

        // Draw the rate level from the last analysis
        int xRD = 0;

        if (p.getRatesDoubles().size() > 0) {
            p.fill(164, 197, 249);
            p.text("Analysis & Spectrum Analysis", 10, 316);
        }

        for (String key : p.getRatesDoubles().keySet()) {
            Integer level = p.getRatesDoubles().get(key);
            p.stroke(0, 25 * level, 0);
            if (key.equals(p.getCp5().get(Textfield.class, "Output").getText())) {
                p.stroke(255);
            }
            p.line(xRD, 720, xRD, 720 - (level * 15));
            p.line(xRD, 760, xRD, 760 - (level * 15));
            xRD++;
            if (xRD >= 599) xRD = 0;
        }

        int yRate = 350;

        for (int iRate = 0; iRate < p.getRateList().size(); iRate++) {

            RateObject rateObject = p.getRateList().get(iRate);

            if (rateObject == null) {
                System.err.println("rateObject or p.getGeneralVitality() is null");
                continue;
            }

            boolean selectRate = false;
            //MOUSELINE
            if (p.mouseY >= yRate - 20 && p.mouseY < yRate && p.mouseX < 600) { //de breedde van de muis bij klikken
                p.fill(10, 255, 10, 120);
                p.noStroke();
                p.rect(0, yRate - 16, 600, 20);
                selectRate = true;
            }

            p.fill(255);
            p.text(rateObject.getEnergeticValue(), 50, yRate); //PLACE OF THE RATE NUMBERS

            if (selectRate) {
                p.fill(255);
            } else if (rateObject.getGv() == 0) { // kleuren
                p.fill(150);
            } else if (p.getGeneralVitality() != null && rateObject.getGv() > p.getGeneralVitality() && rateObject.getGv() > 1000) {
                p.fill(32, 255, 24);
            } else if (p.getGeneralVitality() != null && rateObject.getGv() > p.getGeneralVitality()) {
                p.fill(28, 204, 22);
            } else if (p.getGeneralVitality() != null && rateObject.getGv() < p.getGeneralVitality()) {
                p.fill(255, 105, 30);
            } else {
                p.fill(12, 134, 178);
            }

            if (rateObject.getRecurring() > 0) {
                int red = (int) p.red(p.getGraphics().fillColor);
                int green = (int) p.green(p.getGraphics().fillColor);
                int blue = (int) p.blue(p.getGraphics().fillColor);

                red += rateObject.getRecurring() * 10;
                green += rateObject.getRecurring() * 10;
                blue += rateObject.getRecurring() * 10;

                if (red > 255) red = 255;
                if (green > 255) green = 255;
                if (blue > 255) blue = 255;

                p.fill(red, green, blue);
            }

            //PLACE OF THE RATE TEXT
            p.text(rateObject.getNameOrRate() + " (" + rateObject.getRecurring() + ")", 85, yRate); // was 40 afstand naam van anayse iten van nummers 10 tm einde

            if (rateObject.getGv() != 0) {
                p.fill(208, 147, 255);
                p.text(rateObject.getGv(), 10, yRate);
            }

            yRate += 20;
        }

        p.noStroke();
        // TEXTLINE ANALYSIS
        if (p.getGeneralVitality() == null && p.getRateList().size() > 0) {
            p.fill(135, 223, 255);
            p.text("Check General Vitality as next step!", 10, yRate);
        } else if (p.getGeneralVitality() != null) {
            p.fill(150, 227, 255);
            p.text("General Vitality is " + p.getGeneralVitality(), 10, yRate);
        } else if (p.getSelectedDatabase() != null && p.getRateList().size() == 0) {
            p.fill(66, 214, 47);
            p.text("Focus and then click on ANALYZE", 10, yRate);
        }

        // PHOTOGRAPHY and IMAGES
        int countImage = 0;

        if (p.getTile() != null) {
            p.fill(0);
            p.rect(592, 313, 420, 460);
            p.getTile().drawTile();
            countImage++;
        }

        for (PImage img : p.getPhotos()) {
            if (countImage > 0) {
                p.tint(255, 255 / p.getPhotos().size());
            }
            p.image(img, 592, 315);
            countImage++;
        }

        if (p.getPhotos().size() > 0) {
            p.fill(66, 214, 47);
            p.noTint();
            p.text("Layers of photos: " + p.getPhotos().size(), 592, 706);
        }

        // FIXME
//        paintBroadcastedPixel();

        if (p.isConnectMode() || p.isDisconnectMode()) {
            if (core.getRandomNumber(1000) > 950) {
                p.setProgress(p.getProgress() + 1);
                core.setProgress(p.getProgress());
            }

            if (p.getProgress() >= 100) {

                if (p.isConnectMode()) {
                    p.setMonitorText("CONNECTED!");
                }

                if (p.isDisconnectMode()) {
                    p.setMonitorText("DISCONNECTED!");
                }

                p.setConnectMode(false);
                p.setDisconnectMode(false);
            }
        }
    }

    private void paintBackgroundRect(String groupName, int x, int y, int w, int h) {

        p.rect(
                guiConf.getInteger(String.format("backgroundRectangle.%s.x", groupName), x),
                guiConf.getInteger(String.format("backgroundRectangle.%s.y", groupName), y),
                guiConf.getInteger(String.format("backgroundRectangle.%s.w", groupName), w),
                guiConf.getInteger(String.format("backgroundRectangle.%s.h", groupName), h)
        );
    }

    // FIXME
//    public synchronized void paintBroadcastedPixel() {
//        for (ImagePixel pixel : arduinoConnection.getBroadcastedImagePixels()) {
//            p.stroke(p.color(pixel.b, pixel.g, pixel.r));
//            p.point(pixel.x, pixel.y);
//        }
//    }

    /**
     * Draw LEDs on the gui ...
     */
    public void drawRedLED(String text, int x, int y, int textOffset, boolean on) {
        drawLED(text, x, y, textOffset, on, 255, 0, 0);
    }

    public void drawGreenLED(String text, int x, int y, int textOffset, boolean on) {
        drawLED(text, x, y, textOffset, on, 0, 255, 0);
    }

    public void drawBlueLED(String text, int x, int y, int textOffset, boolean on) {
        drawLED(text, x, y, textOffset, on, 0, 0, 255);
    }

    public void drawLED(String text, int x, int y, int textOffset, boolean on, int r, int g, int b) {
        p.fill(50, 0, 0);
        if (on) {
            p.fill(r, g, b);
        }
        p.stroke(200);
        p.strokeWeight(3);
        p.ellipse(x, y, 30, 30);
        p.strokeWeight(1);

        p.fill(255);
        p.textSize(9);
        p.text(text, x - textOffset, y + 30);
    }
}