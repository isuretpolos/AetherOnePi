package de.isuret.polos.aopi.hotbits;

import java.io.ByteArrayOutputStream;

/** Compares consecutive grayscale frames and discards equal pixels and equal bit pairs. */
public class FrameEntropyExtractor {

    private byte[] previous;
    private int firstBit = -1;
    private int outputByte;
    private int outputBits;

    public byte[] extract(byte[] frame) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        if (previous != null && previous.length == frame.length) {
            for (int index = 0; index < frame.length; index++) {
                int difference = (frame[index] & 0xFF) - (previous[index] & 0xFF);
                if (difference == 0) {
                    continue;
                }
                int bit = difference > 0 ? 1 : 0;
                if (firstBit == -1) {
                    firstBit = bit;
                } else {
                    // Von Neumann extraction: 01 -> 0, 10 -> 1, 00/11 -> discard.
                    if (firstBit != bit) {
                        outputByte = (outputByte << 1) | firstBit;
                        outputBits++;
                        if (outputBits == 8) {
                            result.write(outputByte);
                            outputBits = 0;
                            outputByte = 0;
                        }
                    }
                    firstBit = -1;
                }
            }
        } else {
            firstBit = -1;
            outputBits = 0;
            outputByte = 0;
        }
        previous = frame.clone();
        return result.toByteArray();
    }
}
