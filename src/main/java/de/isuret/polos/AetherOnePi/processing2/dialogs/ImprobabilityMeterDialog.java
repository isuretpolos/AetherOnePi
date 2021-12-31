package de.isuret.polos.AetherOnePi.processing2.dialogs;

import de.isuret.polos.AetherOnePi.processing2.hotbits.HotbitsHandler;
import processing.core.PApplet;

/**
 * Measurement of improbable events in realtime
 */
public class ImprobabilityMeterDialog extends PApplet {

    public static int WIDTH = 1920 / 2;
    public static int HEIGHT = 1000 / 2;
    public static int CENTER = WIDTH / 2;
    public static int BEGIN_Y = 20;
    public static int BOTTOM_Y = HEIGHT - (HEIGHT / 4);
    public static int PIXEL_SIZE = 10;
    public static int MAX_AGE = (BOTTOM_Y - BEGIN_Y) / PIXEL_SIZE;
    private HotbitsHandler hotbitsHandler;
    private Pixel[][] pixels = new Pixel[WIDTH / PIXEL_SIZE][(BOTTOM_Y - BEGIN_Y) / PIXEL_SIZE];

    private class Pixel {

        int x;
        int y;
        int age = 0;

        public Pixel(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) {
        init(new HotbitsHandler(null));
    }

    public static void init(HotbitsHandler hotbitsHandler) {

        ImprobabilityMeterDialog improbabilityMeterDialog = new ImprobabilityMeterDialog(hotbitsHandler);
        String[] args2 = {""};
        PApplet.runSketch(args2, improbabilityMeterDialog);
    }

    public void exit() {
        surface.setVisible(false);
        dispose();
        System.out.println("CLOSED");
    }

    public ImprobabilityMeterDialog(HotbitsHandler hotbitsHandler) {
        System.out.println(pixels.length);
        System.out.println(pixels[0].length);
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

        for (int x = 0; x < pixels.length; x++) {

            pixels[x] = new Pixel[(BOTTOM_Y - BEGIN_Y) / PIXEL_SIZE];

            for (int y = 0; y < pixels[x].length; y++) {
                pixels[x][y] = null;
            }
        }
    }

    public void draw() {
        background(0);

        if (pixels[pixels.length / 2][0] == null) {
            pixels[pixels.length / 2][0] = new Pixel(CENTER, BEGIN_Y);
        }

        for (int x = 0; x < pixels.length; x++) {
            for (int y = pixels[x].length -1; y > -1; y--) {

                Pixel pixel = pixels[x][y];
                System.out.println(y);
                if (pixel == null) continue;

                fill(255, 255 - y * 3, 255 - y * 3);
                rect(pixel.x, pixel.y, PIXEL_SIZE, PIXEL_SIZE);

                if (pixel.y < BOTTOM_Y -  2* PIXEL_SIZE) {
                    pixel.y += PIXEL_SIZE;

                    if (hotbitsHandler.getBoolean()) {
                        if (pixels[x+1][y+1] != null) {
                            pixels[x+1][y+1].age += 1;
                            pixel.age += 1;
                            fill(255 - pixel.age,0,0);
                        } else {
                            pixel.x += PIXEL_SIZE;
                            pixel.age = 0;
                            pixels[x + 1][y + 1] = pixel;
                            pixels[x][y] = null;
                        }
                    } else {
                        if (pixels[x-1][y+1] != null) {
                            pixels[x-1][y+1].age += 1;
                            pixel.age += 1;
                            fill(255 - pixel.age,0,0);
                        } else {
                            pixel.x -= PIXEL_SIZE;
                            pixel.age = 0;
                            pixels[x - 1][y + 1] = pixel;
                            pixels[x][y] = null;
                        }
                    }

                } else {
                    pixel.age += 1;
                    if (pixel.age >= MAX_AGE) {
                        pixels[x][y] = null;
                    }
                    fill(255 - pixel.age,0,0);
                }

                rect(pixel.x, pixel.y, PIXEL_SIZE, PIXEL_SIZE);
            }
        }

        // draw grid
        stroke(25);
        noFill();
        rect(CENTER - (pixels.length / 2) * PIXEL_SIZE, BEGIN_Y, pixels.length * PIXEL_SIZE, (pixels.length * PIXEL_SIZE) - BEGIN_Y);
        for (int x = 0; x < pixels.length; x++) {
            line(x * PIXEL_SIZE, BEGIN_Y, x * PIXEL_SIZE, BOTTOM_Y);
        }

        for (int y = pixels[0].length - 1; y > -1; y--) {
            line(CENTER - (pixels.length / 2) * PIXEL_SIZE, y * PIXEL_SIZE + BEGIN_Y, pixels.length * PIXEL_SIZE, y * PIXEL_SIZE + BEGIN_Y);
        }
    }
}
