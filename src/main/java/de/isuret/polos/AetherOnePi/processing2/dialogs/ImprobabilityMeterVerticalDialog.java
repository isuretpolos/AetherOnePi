package de.isuret.polos.AetherOnePi.processing2.dialogs;

import de.isuret.polos.AetherOnePi.processing2.hotbits.HotbitsHandler;
import processing.core.PApplet;
import processing.core.PFont;

/**
 * Measurement of improbable events in realtime
 */
public class ImprobabilityMeterVerticalDialog extends PApplet {

    public static int WIDTH = 400;
    public static int HEIGHT = 700;
    private HotbitsHandler hotbitsHandler;

    private int cumulativeValue = 0;
    private Integer lastRandomValue = null;

    public static void main(String[] args) {
        init(new HotbitsHandler(null));
    }

    public static void init(HotbitsHandler hotbitsHandler) {

        ImprobabilityMeterVerticalDialog improbabilityMeterDialog = new ImprobabilityMeterVerticalDialog(hotbitsHandler);
        String[] args2 = {""};
        PApplet.runSketch(args2, improbabilityMeterDialog);
    }

    public void exit() {
        surface.setVisible(false);
        dispose();
        System.out.println("CLOSED");
        System.exit(0);
    }

    public ImprobabilityMeterVerticalDialog(HotbitsHandler hotbitsHandler) {
        this.hotbitsHandler = hotbitsHandler;
    }

    public void settings() {
        size(WIDTH, HEIGHT);
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }

    public void setup() {
        background(0);
        surface.setTitle("Improbability Meter");
        PFont font = createFont("Verdana", 20);
        textFont(font);
    }

    public void draw() {

        background(0);

        stroke(255);

        if (lastRandomValue != null && lastRandomValue == 10) {
            stroke(255,0,0);
        }

        text("points: " + cumulativeValue, 20, 30);

        for (int i = 0; i < 24; i++) {

            if (cumulativeValue > i * 10 ) {
                fill(255,i*10,0);
                rect(20, HEIGHT - (14 * i) - 60, WIDTH - 40, 10);
            }
        }

        throwDice();

        if (cumulativeValue > 0) {
            cumulativeValue -= 1;
        }
    }

    public void throwDice() {

        if (lastRandomValue != null && lastRandomValue == 10) {

            cumulativeValue += lastRandomValue;
        }

        lastRandomValue = hotbitsHandler.getInteger(10);
    }
}
