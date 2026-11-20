package de.isuret.polos.aopi.desktop.hotbits;

import java.io.IOException;

/** FT240X comparator extraction with basic continuous checks, not the vendor's entropy estimator. */
public class InfiniteNoiseDecoder {

    private static final int HEALTH_WINDOW_BITS = 8192;
    private static final int WARMUP_BITS = 32768;
    private long totalBits;
    private int windowBits;
    private int ones;
    private int transitions;
    private int previousBit = -1;
    private int runLength;

    public static byte[] clockPattern() {
        byte[] pattern = new byte[512];
        for (int index = 0; index < pattern.length; index++) {
            pattern[index] = (byte) (index % 2 == 0 ? 1 << 2 : 1);
        }
        return pattern;
    }

    public byte[] decode(byte[] samples) throws IOException {
        if (samples.length != 512) {
            throw new IOException("Expected 512 Infinite Noise comparator samples.");
        }
        byte[] output = new byte[samples.length / 8];
        for (int index = 0; index < samples.length; index++) {
            // Reference protocol: comparator 1 (pin 1) on even samples, comparator 2 (pin 4) on odd samples.
            int pin = index % 2 == 0 ? 1 : 4;
            int bit = (samples[index] >> pin) & 1;
            checkBit(bit);
            output[index / 8] = (byte) ((output[index / 8] << 1) | bit);
        }
        return output;
    }

    private void checkBit(int bit) throws IOException {
        if (bit == previousBit) {
            runLength++;
        } else {
            if (previousBit != -1) {
                transitions++;
            }
            runLength = 1;
        }
        previousBit = bit;
        if (runLength > 64) {
            throw new IOException("Infinite Noise health check failed: stuck comparator output.");
        }
        ones += bit;
        windowBits++;
        totalBits++;
        if (windowBits == HEALTH_WINDOW_BITS) {
            double balance = ones / (double) windowBits;
            double transitionRate = transitions / (double) windowBits;
            if (balance < 0.35 || balance > 0.65 || transitionRate < 0.2 || transitionRate > 0.8) {
                throw new IOException("Infinite Noise health check failed: unusual bit balance or transitions.");
            }
            windowBits = 0;
            ones = 0;
            transitions = 0;
        }
    }

    public boolean isWarmedUp() {
        return totalBits >= WARMUP_BITS;
    }
}
