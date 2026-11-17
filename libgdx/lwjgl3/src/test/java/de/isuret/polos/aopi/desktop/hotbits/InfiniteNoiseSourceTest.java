package de.isuret.polos.aopi.desktop.hotbits;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class InfiniteNoiseSourceTest {

    @Test
    void usbClockPatternAlternatesTheTwoSwitchEnablePins() {
        byte[] pattern = InfiniteNoiseDecoder.clockPattern();
        assertEquals(512, pattern.length);
        for (int index = 0; index < pattern.length; index++) {
            assertEquals(index % 2 == 0 ? 4 : 1, pattern[index]);
        }
    }

    @Test
    void decoderUsesCorrectComparatorPhaseAndIgnoresOtherPins() throws IOException {
        byte[] expected = new byte[64];
        Arrays.fill(expected, (byte) 0xA6);
        assertArrayEquals(expected, new InfiniteNoiseDecoder().decode(pinSamples(expected)));
    }

    @Test
    void stuckComparatorAndIncompleteTransfersAreRejected() {
        assertThrows(IOException.class, () -> new InfiniteNoiseDecoder().decode(new byte[512]));
        assertThrows(IOException.class, () -> new InfiniteNoiseDecoder().decode(new byte[511]));
    }

    @Test
    void periodicAlternatingOutputFailsContinuousChecksDespitePerfectBalance() {
        InfiniteNoiseDecoder decoder = new InfiniteNoiseDecoder();
        byte[] alternating = new byte[64];
        Arrays.fill(alternating, (byte) 0xAA);
        assertThrows(IOException.class, () -> {
            for (int index = 0; index < 16; index++) {
                decoder.decode(pinSamples(alternating));
            }
        });
    }

    @Test
    void identityFilterRejectsUnrelatedFtdiBoardsAndMissingSerials() {
        assertTrue(new InfiniteNoiseTransport.DeviceInfo("1337-ABC", "Infinite Noise TRNG", 0x04036015)
                .isInfiniteNoise());
        assertFalse(new InfiniteNoiseTransport.DeviceInfo("ABC", "USB Serial Converter", 0x04036015)
                .isInfiniteNoise());
        assertFalse(new InfiniteNoiseTransport.DeviceInfo("ABC", "Infinite Noise TRNG", 0x04036001)
                .isInfiniteNoise());
        assertFalse(new InfiniteNoiseTransport.DeviceInfo("", "Infinite Noise TRNG", 0x04036015)
                .isInfiniteNoise());
    }

    @Test
    void warmupIsDiscardedAndOnlyLaterSamplesAreReturned() throws IOException {
        TestTransport transport = new TestTransport();
        try (InfiniteNoiseSource source = new InfiniteNoiseSource(transport, "selected-stick")) {
            source.open();
            assertEquals("selected-stick", transport.selectedSerial);
            assertTrue(transport.configured);
            assertEquals(64, transport.transfers);
            assertEquals(64, source.readSample().length);
            assertEquals(65, transport.transfers);
        }
        assertEquals(1, transport.closes);
    }

    @Test
    void configurationFailureClosesTheOpenedConnection() {
        TestTransport transport = new TestTransport() {
            @Override
            public void configure() throws IOException {
                throw new IOException("USB configuration failed");
            }
        };
        try (InfiniteNoiseSource source = new InfiniteNoiseSource(transport, "selected-stick")) {
            assertThrows(IOException.class, source::open);
        }
        assertEquals(1, transport.closes);
    }

    @Test
    void cancelledWarmupStopsAndReleasesTheDevice() {
        TestTransport transport = new TestTransport() {
            @Override
            public byte[] exchange(byte[] clocks) {
                Thread.currentThread().interrupt();
                return super.exchange(clocks);
            }
        };
        try (InfiniteNoiseSource source = new InfiniteNoiseSource(transport, "selected-stick")) {
            assertThrows(IOException.class, source::open);
        } finally {
            Thread.interrupted();
        }
        assertEquals(1, transport.closes);
        assertEquals(1, transport.transfers);
    }

    @Test
    void closingBeforeOpenNeverOpensTheDevice() {
        TestTransport transport = new TestTransport();
        InfiniteNoiseSource source = new InfiniteNoiseSource(transport, "selected-stick");
        source.close();
        assertThrows(IOException.class, source::open);
        assertNull(transport.selectedSerial);
    }

    private static byte[] pinSamples(byte[] bytes) {
        byte[] samples = new byte[bytes.length * 8];
        for (int index = 0; index < samples.length; index++) {
            int bit = (bytes[index / 8] >> (7 - index % 8)) & 1;
            int activePin = index % 2 == 0 ? 1 : 4;
            int otherPin = index % 2 == 0 ? 4 : 1;
            samples[index] = (byte) (0xED | (bit << activePin) | ((1 - bit) << otherPin));
        }
        return samples;
    }

    private static class TestTransport implements InfiniteNoiseTransport, InfiniteNoiseTransport.Connection {
        private final Random fixture = new Random(89);
        private String selectedSerial;
        private boolean configured;
        private int transfers;
        private int closes;

        @Override
        public List<DeviceInfo> listDevices() {
            return List.of();
        }

        @Override
        public Connection open(String serial) {
            selectedSerial = serial;
            return this;
        }

        @Override
        public void configure() throws IOException {
            configured = true;
        }

        @Override
        public byte[] exchange(byte[] clocks) {
            assertArrayEquals(InfiniteNoiseDecoder.clockPattern(), clocks);
            transfers++;
            byte[] bytes = new byte[64];
            fixture.nextBytes(bytes);
            return pinSamples(bytes);
        }

        @Override
        public void close() {
            closes++;
        }
    }
}
