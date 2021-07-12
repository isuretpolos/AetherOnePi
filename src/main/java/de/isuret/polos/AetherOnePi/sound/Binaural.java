package de.isuret.polos.AetherOnePi.sound;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Binaural {
    private static final int SAMPLE_RATE = 16 * 1024; // 16KHz
    private static final int SAMPLE_SIZE = 8; // 8 bits per sample
    private static final int NUM_CHANNELS = 2; // Stereo
    private final byte leftChannel[] = new byte[SAMPLE_RATE]; // 1 sec of samples
    private final byte rightChannel[] = new byte[SAMPLE_RATE];
    private SourceDataLine line = null;
    private final AudioFormat audioFormat = new AudioFormat(
            SAMPLE_RATE,
            SAMPLE_SIZE,
            NUM_CHANNELS,
            true,
            true
    );

    public static void main(String[] args) {
        int leftFreq = 500; // A = 440Hz
        int rightFreq = 508;
        Binaural binauralGenerator = new Binaural(leftFreq, rightFreq, .5f);
        binauralGenerator.play(3); // Play for 3 seconds
        binauralGenerator.shutdown();
    }

    public Binaural(int leftFreq, int rightFreq, float volume) {
        // Fill the left and right channel buffers with sine waves
        fillBufferWithSineWave(leftChannel, leftFreq, volume);
        fillBufferWithSineWave(rightChannel, rightFreq, volume);

        // Initialize audio output
        try {
            line = AudioSystem.getSourceDataLine(audioFormat);
            line.open(audioFormat, (int) audioFormat.getSampleRate());
            line.start();
        } catch (LineUnavailableException ex) {
            Logger.getLogger(Binaural.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }

    private void fillBufferWithSineWave(byte[] buffer, int frequency, float volume) {
        double period = (double) SAMPLE_RATE / frequency;
        for (int i = 0; i < SAMPLE_RATE; i++) { // Fill each byte of the buffer
            double angle = 2.0 * Math.PI * i / period ;
            buffer[i] = (byte) (Math.sin(angle) * 127f * volume);
        }
    }

    public void play(int seconds) {
        int samplePosition = 0;
        int endPosition = (int)audioFormat.getSampleRate();
        for (int i = 0; i < SAMPLE_RATE * seconds / 2; i++) {
            line.write(leftChannel, samplePosition, 2); // 2 bytes from each
            line.write(rightChannel, samplePosition, 2);
            samplePosition += 2;
            if (samplePosition >= endPosition) {
                samplePosition = 0;
            }
        }
    }

    public void shutdown() {
        line.flush();
        line.close();
    }
}
