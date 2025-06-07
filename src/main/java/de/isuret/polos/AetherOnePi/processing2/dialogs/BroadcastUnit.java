package de.isuret.polos.AetherOnePi.processing2.dialogs;

import processing.core.PApplet;
import processing.core.PImage;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * A broadcast unit interweave a word or rate with time and space,
 * drawing the collective consciousness attention to the intention
 * of the operators mind, for the best and good of all.
 */
public class BroadcastUnit extends PApplet {

    public static final int WIDTH = 320;
    public static final int HEIGHT = 180;
    private Integer multiplier;
    private Integer seconds;
    private String signature;
    private Long start;
    private Random random;
    private SecureRandom random2;
    private Integer saveFrames = 0;
    private Integer movingWaveAmount = 0;

    private static Long lastBroadcast = null;

    private List<PImage> imageList;

    public static void main(String[] args) {
        startBroadcastUnit(480, "Sulfur",1);
    }

    public static void startBroadcastUnit(int seconds, String signature, Integer multiplier) {
        startBroadcastUnit(seconds, signature, null, multiplier);
    }
    public static void startBroadcastUnit(int seconds, String signature, List<PImage> imageList, Integer multiplier) {

        Long now = Calendar.getInstance().getTimeInMillis();

        // prevent multiple broadcast units in under 500 milliseconds (click and open multiple dialogs was not intented)
        if (lastBroadcast != null && now < (lastBroadcast + 500)) {
            return;
        }

        BroadcastUnit broadcastUnit = new BroadcastUnit(seconds, signature, multiplier);
        broadcastUnit.imageList = imageList;
        String[] args2 = {""};
        PApplet.runSketch(args2, broadcastUnit);

        if (!new File("images").exists()) {
            new File("images").mkdir();
        }

        lastBroadcast = now;
    }

    public void exit() {
        surface.setVisible(false);
        dispose();
        System.out.println("CLOSED");
    }

    public BroadcastUnit(int seconds, String signature, Integer multiplier) {
        this.seconds = seconds;
        this.signature = signature;
        this.multiplier = multiplier;

        try {
            random2 = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void settings() {
        size(WIDTH, HEIGHT);
    }

    public void setTitle(String title) {
        surface.setTitle(title);
    }

    public void setup() {
        background(200);
        surface.setTitle(signature);
    }

    public void draw() {

        if (seconds == null) return;

        if (signature == null) return;

        if (start == null) {
            start = Calendar.getInstance().getTimeInMillis();
            random = new Random(start);
            background(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        }

        if (Calendar.getInstance().getTimeInMillis() > start + (1000 * seconds)) {
            exit();
        }

        if (random.nextInt(13) >= 12) {
            paintSignatureCharacter();
        } else if (random.nextInt(21) >= 20) {

            int max = random.nextInt(1000) + 1;

            if (max > 90) {
                paintOverlay();
            }

            for (int x = 0; x > max; x++) {
                paintOneLayer();
            }
        } else if (random.nextInt(33) >= 32) {
            paintOverlay();

            int max = random.nextInt(10000) + 1;

            if (max > 9000) {
                paintSignatureText();
            }

            for (int x = 0; x > max; x++) {
                paintOneLayer();
            }
        } else if (random.nextInt(54) >= 53) {
            paintOverlay(20);
        } else if (random.nextInt(89) >= 89) {
            paintOverlay(50);
        }

        paintOneLayer();
        paintProgressBar();

        if (saveFrames > 0) {
            saveFrame("images/radionicBroadcasting-######.png");
            saveFrames--;
        }


    }

    public void paintProgressBar() {
        noStroke();
        fill(255,0,0);

        Float sec = new Float(seconds);
        Float wid = new Float(WIDTH);
        Float millisAfterStart = new Float(Calendar.getInstance().getTimeInMillis() - start);
        Float delta = 100/wid/sec;
        Float progress = millisAfterStart * delta;
        rect(0,0,progress,3);
    }

    private void paintOverlay() {
        paintOverlay(5);
    }

    private void paintOverlay(int alpha) {
        choseRandomColorAlpha(alpha);
        rect(0, 0, WIDTH, HEIGHT);
    }

    private void paintOneLayer() {
        paintPoint();

        if (random2.nextInt(21) >= 20) {
            partialInvert();
        }

        if (random2.nextInt(33) >= 32) {

            int max = random.nextInt(100) + 1;

            for (int x = 0; x > max; x++) {
                paintPoint();
            }
        }

        paintBezier(54, 53);

        if (random2.nextInt(54) >= 53) {

            int max = random.nextInt(100) + 1;

            for (int x = 0; x > max; x++) {
                paintSignatureCharacter();
            }

            max = random.nextInt(1000) + 1;

            for (int x = 0; x > max; x++) {
                paintPoint();
            }
        }

        paintBezier(89, 88);

        if (random2.nextInt(89) >= 88) {
            paintSignatureCharacter();
            partialInvert(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }

        paintLine(144, 143);
        paintArc(144, 143);
        paintBezier(144, 143);
        paintRectangle(144, 143, 4);
        paintEllipse(144, 143, 4);

        paintRectangle(233, 232, 10);
        paintEllipse(233, 232, 10);

        paintRectangle(377, 373, 20);
        paintEllipse(377, 373, 20);

        paintRectangle(610, 609, 40);
        paintEllipse(610, 609, 40);

        paintRectangle(987, 986, 80);
        paintEllipse(987, 986, 80);
        paintTriangle(987, 986);
        paintPolygon(987, 986);

        paintRadionicCard(3, 2);
        paintRadionicCardWave(3, 2);

        if (imageList != null && !imageList.isEmpty()) {
            for (PImage image : imageList) {
                if (random2.nextInt(1000) >= 998) {
                    image.resize(width, height);
                    blend(image, 0, 0, width, height, 0,0, width, height, DIFFERENCE);
                }
            }
        }

        if (random2.nextInt(6765 + multiplier) >= 6764 + multiplier) {
            movingWaveAmount = 1;
        }
    }

    private void paintRadionicCard(int i, int i2) {

        if (random.nextInt(i) >= i2) {
            noFill();
            stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            ellipse(width / 2, height / 2, height - 4, height - 4);
            stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            ellipse(width / 2, height / 2, height - 14, height - 14);
            stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            ellipse(width / 2, height / 2, height - 24, height - 24);

            if (random.nextInt(5) == 1) {
                stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255), random.nextInt(255));
                lineAngle(width / 2, height / 2, random.nextInt(360), (height - 24) / 2);
            }
        }
    }

    private void paintRadionicCardWave(int i, int i2) {

        if (movingWaveAmount == 0) {
            return;
        }

        if (movingWaveAmount > 200) {
            movingWaveAmount = 0;
            return;
        }

        System.out.println(movingWaveAmount);
        movingWaveAmount += 4;

        noFill();
        stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        ellipse(width / 2, height / 2, (height - 4) - movingWaveAmount, (height - 4) - movingWaveAmount);
        stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        ellipse(width / 2, height / 2, (height - 4) + movingWaveAmount, (height - 4) + movingWaveAmount);
    }

    private void lineAngle(int x, int y, float angle, float length) {
        line(x + cos(angle) * length, y - sin(angle) * length, x + cos(angle) * length / 2, y - sin(angle) * length / 2);
    }

    private void partialInvert() {

        loadPixels();

        for (int i = 0; i < (width * height); i++) {

            float red = red(pixels[i]);
            float green = green(pixels[i]);
            float blue = blue(pixels[i]);

            if (random.nextInt(width * height) >= (width * height) - 1) {
                paintOverlay(5);
                break;
            } else if (random.nextInt(width * height) >= (width * height) / 2) {
                red = 250 - red;
                green = 250 - green;
                blue = 250 - blue;
            } else {
                red = 255 - red;
                green = 255 - green;
                blue = 255 - blue;
            }

            pixels[i] = color(red, green, blue);
        }

        updatePixels();
    }

    private void partialInvert(int x, int y, int w, int h) {

        loadPixels();

        int xx = 0;
        int yy = 0;

        for (int i = 0; i < (width * height); i++) {

            xx++;

            if (xx >= width) {
                xx = 0;
                yy++;
            }

            if (!(xx >= x && xx <= x + w && yy >= y && yy <= y + h)) {
                continue;
            }

            float red = red(pixels[i]);
            float green = green(pixels[i]);
            float blue = blue(pixels[i]);
            red = 255 - red;
            green = 255 - green;
            blue = 255 - blue;

            pixels[i] = color(red, green, blue);
        }

        updatePixels();
    }

    private void paintPoint() {
        stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        fill(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        point(random.nextInt(WIDTH), random.nextInt(HEIGHT));
    }

    private void paintLine(int i, int i2) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            line(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
    }

    private void paintArc(int i, int i2) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            noFill();
            arc(random.nextInt(WIDTH), random.nextInt(HEIGHT), 60, 60, HALF_PI, PI);
            arc(random.nextInt(WIDTH), random.nextInt(HEIGHT), 70, 70, PI, PI + QUARTER_PI);
            arc(random.nextInt(WIDTH), random.nextInt(HEIGHT), 80, 80, PI + QUARTER_PI, TWO_PI);
        }
    }

    private void paintBezier(int i, int i2) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            noFill();
            bezier(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
    }

    private void paintRectangle(int i, int i2, int i3) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            rect(random.nextInt(WIDTH), random.nextInt(HEIGHT), i3, i3);
        }
    }

    private void paintEllipse(int i, int i2, int i3) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            ellipse(random.nextInt(WIDTH), random.nextInt(HEIGHT), i3, i3);
        }
    }

    private void paintTriangle(int i, int i2) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            triangle(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
    }

    private void paintPolygon(int i1, int i2) {
        if (random.nextInt(i1) >= i2) {
            choseRandomColorAlpha();
            beginShape();
            for (int c = 0; c < random.nextInt(50) + 3; c++) {
                vertex(random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }
            endShape();
        }
    }

    private void choseRandomColorAlpha(int alpha) {
        stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255), alpha);
        fill(random.nextInt(255), random.nextInt(255), random.nextInt(255), alpha);
    }

    private void choseRandomColorAlpha() {
        stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255), random.nextInt(255));
        fill(random.nextInt(255), random.nextInt(255), random.nextInt(255), random.nextInt(255));
    }

    private void paintSignatureCharacter() {
        paintPoint();

        for (int c = 0; c < random.nextInt(9) + 1; c++) {
            int pos = random.nextInt(signature.length());
            String one = signature.substring(pos, pos + 1);
            textSize(12 + random.nextInt(multiplier));
            text(one, random.nextInt(WIDTH), random.nextInt(HEIGHT));
            textSize(12);
        }
    }

    private void paintSignatureText() {
        paintPoint();
        text(signature, random.nextInt(WIDTH), random.nextInt(HEIGHT));
    }

    public void mousePressed() {
        saveFrames = 100;
    }

    public Integer getSeconds() {
        return seconds;
    }

    public void setSeconds(Integer seconds) {
        this.seconds = seconds;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public SecureRandom getRandom2() {
        return random2;
    }

    public void setRandom2(SecureRandom random2) {
        this.random2 = random2;
    }

    public Integer getSaveFrames() {
        return saveFrames;
    }

    public void setSaveFrames(Integer saveFrames) {
        this.saveFrames = saveFrames;
    }

    public Integer getMovingWaveAmount() {
        return movingWaveAmount;
    }

    public void setMovingWaveAmount(Integer movingWaveAmount) {
        this.movingWaveAmount = movingWaveAmount;
    }

    public static Long getLastBroadcast() {
        return lastBroadcast;
    }

    public static void setLastBroadcast(Long lastBroadcast) {
        BroadcastUnit.lastBroadcast = lastBroadcast;
    }
}
