package de.isuret.polos.AetherOnePi.sound;

import javax.sound.sampled.*;
import java.util.function.Consumer;

public class BinauralSweep {

    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE = 16;
    private static final int NUM_CHANNELS = 2;
    private static final int BYTES_PER_SAMPLE = SAMPLE_SIZE / 8;
    private static final int FRAME_SIZE = NUM_CHANNELS * BYTES_PER_SAMPLE;

    private static final double BEAT_HZ = 4.0;
    private static final double NYQUIST = SAMPLE_RATE / 2.0;
    private static final double FADE_SECONDS = 0.08;
    private static final double FADE_STEP = 1.0 / (SAMPLE_RATE * FADE_SECONDS);
    // ~100 ms line buffer: keeps stop latency low
    private static final int LINE_BUFFER_BYTES = SAMPLE_RATE * FRAME_SIZE / 10;

    private final double minFreq;
    private final double maxFreq;
    private final double loopTime;
    private final double layerShiftHz;
    private final float volume;
    private final boolean alternateSides;

    private volatile boolean running;
    private volatile Throwable error;
    private volatile Consumer<Throwable> onError;
    private Thread thread;

    public BinauralSweep(double minFreq,
                         double maxFreq,
                         float volume,
                         double loopTime,
                         boolean alternateSides,
                         double layerShiftHz) {

        if (Double.isNaN(minFreq) || minFreq <= 0)
            throw new IllegalArgumentException("minFreq must be > 0");

        if (Double.isNaN(maxFreq) || maxFreq <= minFreq)
            throw new IllegalArgumentException("maxFreq must be > minFreq");

        if (Double.isNaN(layerShiftHz) || Double.isInfinite(layerShiftHz) || layerShiftHz < 0)
            throw new IllegalArgumentException("layerShiftHz must be >= 0 and finite");

        if (minFreq - layerShiftHz <= 0)
            throw new IllegalArgumentException("minFreq - layerShiftHz must be > 0");

        if (maxFreq + layerShiftHz + BEAT_HZ >= NYQUIST)
            throw new IllegalArgumentException(
                    "maxFreq + layerShiftHz + " + BEAT_HZ + " must be < " + NYQUIST);

        if (Double.isNaN(loopTime) || Double.isInfinite(loopTime) || loopTime <= 0)
            throw new IllegalArgumentException("loopTime must be > 0 and finite");

        this.minFreq = minFreq;
        this.maxFreq = maxFreq;
        this.volume = Float.isNaN(volume) ? 0f : Math.max(0f, Math.min(1f, volume));
        this.loopTime = loopTime;
        this.alternateSides = alternateSides;
        this.layerShiftHz = layerShiftHz;
    }

    public BinauralSweep(double minFreq,
                         double maxFreq,
                         float volume,
                         boolean alternateSides,
                         double layerShiftHz) {

        this(minFreq, maxFreq, volume, 10.0, alternateSides, layerShiftHz);
    }

    /** Optional callback, invoked from the audio thread if playback fails. */
    public void setOnError(Consumer<Throwable> onError) {
        this.onError = onError;
    }

    public boolean isRunning() {
        return running;
    }

    /** Last error from the audio thread, or null. */
    public Throwable getError() {
        return error;
    }

    public synchronized void start() {
        // Never start a second thread while the old one is still alive
        if (thread != null && thread.isAlive())
            return;

        error = null;
        running = true;

        thread = new Thread(this::run, "BinauralSweep");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void end() {
        running = false;

        if (thread != null) {
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // If it is still alive (fading out / blocked), keep the reference
            // so start() will refuse to spawn a duplicate.
            if (!thread.isAlive())
                thread = null;
        }
    }

    private void run() {

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE,
                SAMPLE_SIZE,
                NUM_CHANNELS,
                FRAME_SIZE,
                SAMPLE_RATE,
                false
        );

        SourceDataLine line = null;

        try {
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, LINE_BUFFER_BYTES);
            line.start();

            final int chunkSamples = 1024;
            byte[] buffer = new byte[chunkSamples * FRAME_SIZE];

            double[] leftPhase = new double[3];
            double[] rightPhase = new double[3];
            double amp = Short.MAX_VALUE * volume;

            double leftPower = 0.25;
            double rightPower = 0.25;
            final double rmsAlpha = 0.002;
            final double targetRms = 0.5;
            final double maxNormalizationGain = 2.0;

            long sampleCounter = 0;
            long samplesPerLoop = Math.max(1, (long) (SAMPLE_RATE * loopTime));

            boolean offsetRight = true;

            while (true) {

                int index = 0;
                boolean wantSound = running;

                for (int i = 0; i < chunkSamples; i++) {

                    long loopSample = sampleCounter % samplesPerLoop;

                    if (loopSample == 0 && sampleCounter > 0 && alternateSides)
                        offsetRight = !offsetRight;

                    double position = (double) loopSample / samplesPerLoop;

                    // 0.0 -> min, 0.5 -> max, 1.0 -> min (smooth cosine)
                    double sweep = (1.0 - Math.cos(position * Math.PI * 2.0)) / 2.0;
                    double baseFreq = minFreq + (maxFreq - minFreq) * sweep;

                    double[] frequencies = {
                            baseFreq - layerShiftHz,
                            baseFreq,
                            baseFreq + layerShiftHz
                    };

                    double leftValue = 0.0;
                    double rightValue = 0.0;

                    for (int layer = 0; layer < 3; layer++) {
                        double leftFreq = frequencies[layer];
                        double rightFreq = frequencies[layer];

                        if (offsetRight)
                            rightFreq += BEAT_HZ;
                        else
                            leftFreq += BEAT_HZ;

                        leftPhase[layer] += 2.0 * Math.PI * leftFreq / SAMPLE_RATE;
                        rightPhase[layer] += 2.0 * Math.PI * rightFreq / SAMPLE_RATE;

                        if (leftPhase[layer] >= Math.PI * 2)
                            leftPhase[layer] -= Math.PI * 2;

                        if (rightPhase[layer] >= Math.PI * 2)
                            rightPhase[layer] -= Math.PI * 2;

                        leftValue += Math.sin(leftPhase[layer]);
                        rightValue += Math.sin(rightPhase[layer]);
                    }

                    leftPower += rmsAlpha * (leftValue * leftValue - leftPower);
                    rightPower += rmsAlpha * (rightValue * rightValue - rightPower);

                    double leftNormalization =
                            targetRms / Math.sqrt(Math.max(leftPower, 1.0e-9));
                    double rightNormalization =
                            targetRms / Math.sqrt(Math.max(rightPower, 1.0e-9));

                    leftNormalization = Math.min(leftNormalization, maxNormalizationGain);
                    rightNormalization = Math.min(rightNormalization, maxNormalizationGain);

                    leftValue *= leftNormalization;
                    rightValue *= rightNormalization;

                    leftValue = Math.max(-1.0, Math.min(1.0, leftValue));
                    rightValue = Math.max(-1.0, Math.min(1.0, rightValue));


                    short leftSample = (short) (leftValue * amp);
                    short rightSample = (short) (rightValue * amp);

                    buffer[index++] = (byte) (leftSample & 0xff);
                    buffer[index++] = (byte) ((leftSample >> 8) & 0xff);

                    buffer[index++] = (byte) (rightSample & 0xff);
                    buffer[index++] = (byte) ((rightSample >> 8) & 0xff);

                    sampleCounter++;
                }

                line.write(buffer, 0, index);

                // Leave only once the fade-out has reached silence
                if (!wantSound)
                    break;
            }

            line.drain();

        } catch (Exception t) {
            error = t;
            Consumer<Throwable> handler = onError;
            if (handler != null) {
                try {
                    handler.accept(t);
                } catch (Exception ignored) {}
            }
        } finally {
            closeLine(line);
            running = false;
        }
    }

    private static void closeLine(SourceDataLine line) {
        if (line == null)
            return;

        try { line.stop(); } catch (Exception ignored) {}
        try { line.flush(); } catch (Exception ignored) {}
        try { line.close(); } catch (Exception ignored) {}
    }
}