package de.isuret.polos.AetherOnePi.sound;

import javax.sound.sampled.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Binaural {

    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE = 16; // 16-bit sounds much better
    private static final int NUM_CHANNELS = 2;
    private static final int BYTES_PER_SAMPLE = SAMPLE_SIZE / 8;
    private static final int FRAME_SIZE = NUM_CHANNELS * BYTES_PER_SAMPLE;

    private final AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            SAMPLE_RATE,
            SAMPLE_SIZE,
            NUM_CHANNELS,
            FRAME_SIZE,
            SAMPLE_RATE,
            false // little endian
    );

    private SourceDataLine line;
    private volatile boolean running;

    private final int leftFreq;
    private final int rightFreq;
    private final float volume;

    public Binaural(int leftFreq, int rightFreq, float volume) {
        this.leftFreq = leftFreq;
        this.rightFreq = rightFreq;
        this.volume = Math.max(0f, Math.min(1f, volume));
    }

    public void play(float seconds) {
        try {
            line = AudioSystem.getSourceDataLine(audioFormat);
            line.open(audioFormat, SAMPLE_RATE * FRAME_SIZE / 2);
            line.start();

            running = true;

            int totalSamples = (int) (SAMPLE_RATE * seconds);
            int chunkSamples = 1024;
            byte[] buffer = new byte[chunkSamples * FRAME_SIZE];

            double leftPhase = 0.0;
            double rightPhase = 0.0;
            double leftStep = 2.0 * Math.PI * leftFreq / SAMPLE_RATE;
            double rightStep = 2.0 * Math.PI * rightFreq / SAMPLE_RATE;

            int fadeSamples = Math.max(1, SAMPLE_RATE / 100); // about 10 ms at 44100 Hz

            int produced = 0;
            while (running && produced < totalSamples) {
                int samplesThisChunk = Math.min(chunkSamples, totalSamples - produced);
                int index = 0;

                for (int i = 0; i < samplesThisChunk; i++) {
                    int globalSampleIndex = produced + i;

                    float envelope = 1.0f;

                    if (globalSampleIndex < fadeSamples) {
                        envelope = (float) globalSampleIndex / fadeSamples;
                    } else if (globalSampleIndex >= totalSamples - fadeSamples) {
                        envelope = (float) (totalSamples - globalSampleIndex) / fadeSamples;
                    }

                    if (envelope < 0f) {
                        envelope = 0f;
                    }

                    short leftSample = (short) (Math.sin(leftPhase) * Short.MAX_VALUE * volume * envelope);
                    short rightSample = (short) (Math.sin(rightPhase) * Short.MAX_VALUE * volume * envelope);

                    buffer[index++] = (byte) (leftSample & 0xff);
                    buffer[index++] = (byte) ((leftSample >> 8) & 0xff);
                    buffer[index++] = (byte) (rightSample & 0xff);
                    buffer[index++] = (byte) ((rightSample >> 8) & 0xff);

                    leftPhase += leftStep;
                    rightPhase += rightStep;

                    if (leftPhase > Math.PI * 2) leftPhase -= Math.PI * 2;
                    if (rightPhase > Math.PI * 2) rightPhase -= Math.PI * 2;
                }

                line.write(buffer, 0, samplesThisChunk * FRAME_SIZE);
                produced += samplesThisChunk;
            }

            line.drain();
        } catch (LineUnavailableException ex) {
            Logger.getLogger(Binaural.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            shutdown();
        }
    }

    public void stop() {
        running = false;
    }

    public void shutdown() {
        running = false;
        if (line != null) {
            try {
                line.stop();
            } catch (Exception ignored) {
            }
            try {
                line.flush();
            } catch (Exception ignored) {
            }
            try {
                line.close();
            } catch (Exception ignored) {
            }
            line = null;
        }
    }
}