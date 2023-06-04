package de.isuret.polos.AetherOnePi.processing2.elements;

import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.domain.ResonanceObject;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.sound.Binaural;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

/**
 * A broadcast element interweave a word or rate with time and space,
 * drawing the collective consciousness attention to the intention
 * of the operators mind, for the best and good of all.
 */
public class BroadcastElement implements IDrawableElement {

    public static final int WIDTH = 320;
    public static final int HEIGHT = 180;
    private static final String ADDITIONAL_RATES[] = {
            "ENERGY", "FIRE", "POWER", "WOOD", "GROUNDING", "EARTH", "RHYTHM",
            "METAL", "WATER", "DEEPNESS", "DO NO HARM!", "UNITY", "LOVE", "BALANCE"
    };
    private AetherOneUI p;
    private String tabName;
    private Integer seconds;
    private String signature;
    private String target;
    private Long start;
    private Random random;
    private SecureRandom random2;
    private Integer saveFrames = 0;
    private Integer movingWaveAmount = 0;
    private Integer offsetX = 0;
    private Integer offsetY = 0;
    private Float progress;
    private Integer counterCheckGV = 500;
    private Boolean counterCheck = false;

    private static Long lastBroadcast = null;
    private boolean stop = false;

    private Binaural binaural = null;
    private boolean playingSound = false;
    private boolean dynamicAdjustments = false;

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

        start();
        random = new Random(start);

        File hotbitsFolder = new File("hotbits");

        if (hotbitsFolder.listFiles().length > 100) {
            random = new Random(p.getHotbitsClient().getInteger(0, 100000));
        }

        p.fill(random.nextInt(255), random.nextInt(255), random.nextInt(255));

        if (!p.getSettings().getBoolean(SettingsScreen.PLAY_SOUND, false)) {
            playingSound = true;
        }

        dynamicAdjustments = p.getSettings().getBoolean(SettingsScreen.DYNAMIC_ADJUSTMENTS, false);
    }

    public void start() {
        start = Calendar.getInstance().getTimeInMillis();
    }

    /**
     * Draws the broadcast signature on a specific area outside the broadcast tab, in order to keep the flow of
     * information without interuption
     *
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public void draw(int x, int y, int width, int height) {
        paintSignatureCharacter(x, y, width, height);
    }

    /**
     * Draws the broadcast signature embedded onto it's own field inside the broadcast tab
     */
    public void draw() {

        if (seconds == null) return;
        if (signature == null) return;

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
        playBinauralSound();
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

    public void calcuateProgress() {
        Float sec = new Float(seconds);
        Float wid = new Float(WIDTH);
        Float millisAfterStart = new Float(Calendar.getInstance().getTimeInMillis() - start);
        Float delta = 100 / wid / sec;
        progress = millisAfterStart * delta;

        if (Calendar.getInstance().getTimeInMillis() > start + (1000 * seconds)) {
            stop = true;
        }
    }

    public void paintProgressBar() {
        calcuateProgress();
        p.noStroke();
        p.fill(255, 0, 0, 50);
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

            boolean foundResonatedRate = false;

            // For every resonance one resonanceObject with dateTime
            ResonanceObject resonanceObject = new ResonanceObject();
            RateObject r = new RateObject();
            r.setNameOrRate(signature);
            resonanceObject.setRateObject(r);
            p.getResonanceList().add(resonanceObject);

            // Cumulative
            for (RateObject rateObject : p.getResonatedList()) {
                if (rateObject.getNameOrRate().equals(signature)) {
                    foundResonatedRate = true;
                    rateObject.setResonateCounter(rateObject.getResonateCounter() + 1);
                }
            }

            if (!foundResonatedRate) {
                RateObject rateObject = new RateObject();
                rateObject.setNameOrRate(signature);
                rateObject.setResonateCounter(1);
                p.getResonatedList().add(rateObject);
            }
        }

        // use hotbits for broadcasting
        int hotbitsForBroadcastingRandomNumber = random.nextInt(100);
        if (hotbitsForBroadcastingRandomNumber >= 98) {
            File hotbitsFolder = new File("hotbits");

            //p.text(one, random.nextInt(width) + offsetX, random.nextInt(height) + offsetY);
            p.text(ADDITIONAL_RATES[p.getHotbitsClient().getInteger(0, ADDITIONAL_RATES.length - 1)],
                    p.getHotbitsClient().getInteger(WIDTH) + offsetX,
                    p.getHotbitsClient().getInteger(HEIGHT) + offsetY);

            int hotbitsCacheSize = hotbitsFolder.listFiles().length;

            if (hotbitsCacheSize > 10000) {

                choseRandomColorAlpha();
                p.rect(10 + offsetX, 10 + offsetY, 10, 10);
                p.text(hotbitsForBroadcastingRandomNumber, 30 + offsetX, 10 + offsetY);

                choseRandomColorAlpha();
                hotbitsForBroadcastingRandomNumber = p.getHotbitsClient().getInteger(0, 1000);
                p.text(hotbitsForBroadcastingRandomNumber, 30 + offsetX, 30 + offsetY);

                if (hotbitsForBroadcastingRandomNumber >= 998) {
                    movingWaveAmount = 1;
                    /*try {
                        SoundFile soundFile = new SoundFile(p, "sounds/wave_F.mp3");
                        soundFile.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/
                    random = new Random(p.getHotbitsClient().getInteger(0, 100000));
                }
            }

            if (hotbitsCacheSize > 20000) {
                random = new Random(p.getHotbitsClient().getInteger(0, 10000000));
            }
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

        movingWaveAmount += 4;

        p.noFill();
        p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        p.ellipse((WIDTH / 2) + offsetX, (HEIGHT / 2) + offsetY, (HEIGHT - 4) - movingWaveAmount, (HEIGHT - 4) - movingWaveAmount);
        p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        p.ellipse((WIDTH / 2) + offsetX, (HEIGHT / 2) + offsetY, (HEIGHT - 4) + movingWaveAmount, (HEIGHT - 4) + movingWaveAmount);

        Collections.sort(p.getResonatedList(), new Comparator<RateObject>() {
            @Override
            public int compare(RateObject o1, RateObject o2) {
                return o2.getResonateCounter().compareTo(o1.getResonateCounter());
            }
        });
    }

    private void playBinauralSound() {
        if (!playingSound && movingWaveAmount > 0) {
            playingSound = true;

            if (binaural == null) {
                int leftFreq = 50 + p.getHotbitsClient().getInteger(500);
                int rightFreq = leftFreq + p.getHotbitsClient().getInteger(50);
                binaural = new Binaural(leftFreq, rightFreq, 0.1f + (p.getHotbitsClient().getInteger(10)* 0.1f));
            }

            (new Thread() {
                public void run() {
                    binaural.play(3);
                    binaural.shutdown();
                    binaural = null;
                    playingSound = false;
                }
            }).start();
        }
    }

    private void lineAngle(int x, int y, float angle, float length) {
        p.line(x + offsetX + p.cos(angle) * length, y + offsetY - p.sin(angle) * length, x + +offsetX + p.cos(angle) * length / 2, y + offsetY - p.sin(angle) * length / 2);
    }

    // TODO add offset
    private void partialInvert(int x, int y, int w, int h) {

        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void paintPoint() {
        paintPoint(offsetX, offsetY, WIDTH, HEIGHT);
    }

    private void paintPoint(int offsetX, int offsetY, int width, int height) {
        p.stroke(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        p.fill(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        p.point(random.nextInt(width) + offsetX, random.nextInt(height) + offsetY);
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
        paintSignatureCharacter(offsetX, offsetY, WIDTH, HEIGHT);
    }

    private void paintSignatureCharacter(int offsetX, int offsetY, int width, int height) {
        paintPoint(offsetX, offsetY, width, height);

        for (int c = 0; c < random.nextInt(9) + 1; c++) {
            int pos = random.nextInt(signature.length());
            String one = signature.substring(pos, pos + 1);
            p.text(one, random.nextInt(width) + offsetX, random.nextInt(height) + offsetY);
        }
    }

    private void paintSignatureText() {
        paintPoint();
        p.text(signature, random.nextInt(WIDTH) + offsetX, random.nextInt(HEIGHT) + offsetY);
    }

    public void mousePressed() {
        saveFrames = 100;
    }

    public AetherOneUI getP() {
        return p;
    }

    public void setP(AetherOneUI p) {
        this.p = p;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
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

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
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

    public Integer getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(Integer offsetX) {
        this.offsetX = offsetX;
    }

    public Integer getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(Integer offsetY) {
        this.offsetY = offsetY;
    }

    public Float getProgress() {
        return progress;
    }

    public void setProgress(Float progress) {
        this.progress = progress;
    }

    public Integer getCounterCheckGV() {
        return counterCheckGV;
    }

    public void setCounterCheckGV(Integer counterCheckGV) {
        this.counterCheckGV = counterCheckGV;
    }

    public Boolean getCounterCheck() {
        return counterCheck;
    }

    public void setCounterCheck(Boolean counterCheck) {
        this.counterCheck = counterCheck;
    }

    public static Long getLastBroadcast() {
        return lastBroadcast;
    }

    public static void setLastBroadcast(Long lastBroadcast) {
        BroadcastElement.lastBroadcast = lastBroadcast;
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public Binaural getBinaural() {
        return binaural;
    }

    public void setBinaural(Binaural binaural) {
        this.binaural = binaural;
    }

    public boolean isPlayingSound() {
        return playingSound;
    }

    public void setPlayingSound(boolean playingSound) {
        this.playingSound = playingSound;
    }

    public boolean isDynamicAdjustments() {
        return dynamicAdjustments;
    }

    public void setDynamicAdjustments(boolean dynamicAdjustments) {
        this.dynamicAdjustments = dynamicAdjustments;
    }
}
