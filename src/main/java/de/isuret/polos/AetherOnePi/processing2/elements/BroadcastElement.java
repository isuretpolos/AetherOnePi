package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import lombok.Getter;
import lombok.Setter;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Random;

/**
 * A broadcast element interweave a word or rate with time and space,
 * drawing the collective consciousness attention to the intention
 * of the operators mind, for the best and good of all.
 */
public class BroadcastElement implements IDrawableElement {

    public static final int WIDTH = 320;
    public static final int HEIGHT = 180;
    private AetherOneUI p;
    private String tabName;
    @Setter
    private Integer seconds;
    @Setter
    private String signature;
    private Long start;
    private Random random;
    private SecureRandom random2;
    private Integer saveFrames = 0;
    private Integer movingWaveAmount = 0;
    private Integer offsetX = 0;
    private Integer offsetY = 0;

    private static Long lastBroadcast = null;
    @Getter
    private boolean stop = false;

    public BroadcastElement(AetherOneUI p, String tabName, int seconds, String signature) {
        this.p = p;
        this.seconds = seconds;
        this.signature = signature;
        this.tabName = tabName;

        try {
            random2 = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Long now = Calendar.getInstance().getTimeInMillis();

        // prevent multiple broadcast units in under 500 milliseconds (click and open multiple dialogs was not intented)
        if (lastBroadcast != null && now < (lastBroadcast + 500)) {
            return;
        }
    }

    public void draw() {

        if (seconds == null) return;

        if (signature == null) return;

        if (start == null) {
            start = Calendar.getInstance().getTimeInMillis();
            random = new Random(start);
            p.fill(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        }

        if (Calendar.getInstance().getTimeInMillis() > start + (1000 * seconds)) {
            stop = true;
            return;
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

    }

    @Override
    public void setDrawOrderByType(int i) {
        offsetX = 40;
        offsetY = 120;

        if (i >= 4) {
            offsetX += (i - 4) * WIDTH;
            offsetY += HEIGHT;
        } else if (i > 0) {
            offsetX += (i * WIDTH);
        }

    }

    @Override
    public String getAssignedTabName() {
        return tabName;
    }

    public void paintProgressBar() {
        p.noStroke();
        p.fill(255, 0, 0, 50);

        Float sec = new Float(seconds);
        Float wid = new Float(WIDTH);
        Float millisAfterStart = new Float(Calendar.getInstance().getTimeInMillis() - start);
        Float delta = 100 / wid / sec;
        Float progress = millisAfterStart * delta;
        p.rect(0 + offsetX, 0 + offsetY, progress, 3);
    }

    private void paintOverlay() {
        paintOverlay(5);
    }

    private void paintOverlay(int alpha) {
        choseRandomColorAlpha(alpha);
        p.rect(0 + offsetX, 0 + offsetY, WIDTH, HEIGHT);
    }

    private void paintOneLayer() {
        paintPoint();

//        if (random2.nextInt(21) >= 20) {
//            partialInvert();
//        }

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
            partialInvert(random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY, random.nextInt(WIDTH), random.nextInt(HEIGHT));
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

        if (random2.nextInt(6765) >= 6764) {
            movingWaveAmount = 1;
        }
    }

    private void paintRadionicCard(int i, int i2) {

        if (random.nextInt(i) >= i2) {
            p.noFill();
            p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            p.ellipse((WIDTH / 2) + offsetX, (HEIGHT / 2) + offsetY, HEIGHT - 4, HEIGHT - 4);
            p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            p.ellipse((WIDTH / 2) + offsetX, (HEIGHT / 2) + offsetY, HEIGHT - 14, HEIGHT - 14);
            p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            p.ellipse((WIDTH / 2) + offsetX, (HEIGHT / 2) + offsetY, HEIGHT - 24, HEIGHT - 24);

            if (random.nextInt(5) == 1) {
                p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255), random.nextInt(255));
                lineAngle(WIDTH / 2, HEIGHT / 2, random.nextInt(360), (HEIGHT - 24) / 2);
            }
        }
    }

    private void paintRadionicCardWave(int i, int i2) {

        if (movingWaveAmount == 0) {
            return;
        }

        if (movingWaveAmount > 2584) {
            movingWaveAmount = 0;
            return;
        }

        System.out.println(movingWaveAmount);
        movingWaveAmount += 4;

        p.noFill();
        p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        p.ellipse((WIDTH / 2) + offsetX, (HEIGHT / 2) + offsetY, (HEIGHT - 4) - movingWaveAmount, (HEIGHT - 4) - movingWaveAmount);
        p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        p.ellipse((WIDTH / 2) + offsetX, (HEIGHT / 2) + offsetY, (HEIGHT - 4) + movingWaveAmount, (HEIGHT - 4) + movingWaveAmount);
    }

    private void lineAngle(int x, int y, float angle, float length) {
        p.line(x + offsetX + p.cos(angle) * length, y + offsetY - p.sin(angle) * length, x + +offsetX + p.cos(angle) * length / 2, y + offsetY - p.sin(angle) * length / 2);
    }

    private void partialInvert() {

        p.loadPixels();

        for (int i = 0; i < (WIDTH * HEIGHT); i++) {

            float red = p.red(p.pixels[i]);
            float green = p.green(p.pixels[i]);
            float blue = p.blue(p.pixels[i]);

            if (random.nextInt(WIDTH * HEIGHT) >= (WIDTH * HEIGHT) - 1) {
                paintOverlay(5);
                break;
            } else if (random.nextInt(WIDTH * HEIGHT) >= (WIDTH * HEIGHT) / 2) {
                red = 250 - red;
                green = 250 - green;
                blue = 250 - blue;
            } else {
                red = 255 - red;
                green = 255 - green;
                blue = 255 - blue;
            }

            p.pixels[i] = p.color(red, green, blue);
        }

        p.updatePixels();
    }

    // TODO add offset
    private void partialInvert(int x, int y, int w, int h) {

        p.loadPixels();

        int xx = 0;
        int yy = 0;

        for (int i = 0; i < (WIDTH * HEIGHT); i++) {

            xx++;

            if (xx >= WIDTH) {
                xx = 0;
                yy++;
            }

            if (!(xx >= x && xx <= x + w && yy >= y && yy <= y + h)) {
                continue;
            }

            float red = p.red(p.pixels[i]);
            float green = p.green(p.pixels[i]);
            float blue = p.blue(p.pixels[i]);
            red = 255 - red;
            green = 255 - green;
            blue = 255 - blue;

            p.pixels[i] = p.color(red, green, blue);
        }

        p.updatePixels();
    }

    private void paintPoint() {
        p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        p.fill(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        p.point(random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY);
    }

    private void paintLine(int i, int i2) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            p.line(random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY, random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY);
        }
    }

    private void paintArc(int i, int i2) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            p.noFill();
            p.arc(random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY, 60, 60, p.HALF_PI, p.PI);
            p.arc(random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY, 70, 70, p.PI, p.PI + p.QUARTER_PI);
            p.arc(random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY, 80, 80, p.PI + p.QUARTER_PI, p.TWO_PI);
        }
    }

    private void paintBezier(int i, int i2) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            p.noFill();
            p.bezier(random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY, random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY, random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY, random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY);
        }
    }

    private void paintRectangle(int i, int i2, int i3) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            p.rect(random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY, i3, i3);
        }
    }

    private void paintEllipse(int i, int i2, int i3) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            p.ellipse(random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY, i3, i3);
        }
    }

    private void paintTriangle(int i, int i2) {
        if (random.nextInt(i) >= i2) {
            choseRandomColorAlpha();
            p.triangle(random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY,
                    random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY,
                    random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY);
        }
    }

    private void paintPolygon(int i1, int i2) {
        if (random.nextInt(i1) >= i2) {
            choseRandomColorAlpha();
            p.beginShape();
            for (int c = 0; c < random.nextInt(50) + 3; c++) {
                p.vertex(random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY);
            }
            p.endShape();
        }
    }

    private void choseRandomColorAlpha(int alpha) {
        p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255), alpha);
        p.fill(random.nextInt(255), random.nextInt(255), random.nextInt(255), alpha);
    }

    private void choseRandomColorAlpha() {
        p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255), random.nextInt(255));
        p.fill(random.nextInt(255), random.nextInt(255), random.nextInt(255), random.nextInt(255));
    }

    private void paintSignatureCharacter() {
        paintPoint();

        for (int c = 0; c < random.nextInt(9) + 1; c++) {
            int pos = random.nextInt(signature.length());
            String one = signature.substring(pos, pos + 1);
            p.text(one, random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY);
        }
    }

    private void paintSignatureText() {
        paintPoint();
        p.text(signature, random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY);
    }

    public void mousePressed() {
        saveFrames = 100;
    }
}
