package de.isuret.polos.aopi.hotbits;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.IntConsumer;

/** Conditions 256 input bytes into eight nonnegative 31-bit integers; never expands a PRNG seed. */
public class EntropyConditioner {

    private final byte[] block = new byte[256];
    private final MessageDigest digest;
    private byte[] lastDigest;
    private int position;
    private int lastByte = -1;
    private int repetitions;

    public EntropyConditioner() {
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime.", exception);
        }
    }

    public void accept(byte[] sample, IntConsumer output) {
        for (byte value : sample) {
            int unsigned = value & 0xFF;
            repetitions = unsigned == lastByte ? repetitions + 1 : 1;
            lastByte = unsigned;
            if (repetitions >= 1024) {
                throw new IllegalStateException("Source health check failed: repeated identical bytes.");
            }
            block[position++] = value;
            if (position == block.length) {
                byte[] conditioned = digest.digest(block);
                if (Arrays.equals(conditioned, lastDigest)) {
                    throw new IllegalStateException("Source health check failed: repeated input blocks.");
                }
                lastDigest = conditioned;
                position = 0;
                ByteBuffer bytes = ByteBuffer.wrap(conditioned);
                while (bytes.hasRemaining()) {
                    output.accept(bytes.getInt() & Integer.MAX_VALUE);
                }
            }
        }
    }
}
