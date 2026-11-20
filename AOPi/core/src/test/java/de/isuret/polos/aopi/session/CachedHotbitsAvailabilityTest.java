package de.isuret.polos.aopi.session;

import de.isuret.polos.aopi.hotbits.HotbitsBackend;
import de.isuret.polos.aopi.hotbits.HotbitsPackageStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CachedHotbitsAvailabilityTest {
    @TempDir
    Path directory;

    @Test
    void boundedDrawMatchesAopiForEachIndependentSeed() throws Exception {
        int[] seeds = {0, 1, -1, 42, 123456, Integer.MIN_VALUE, Integer.MAX_VALUE};
        java.nio.file.Files.writeString(directory.resolve("hotbits_seeds.json"),
                "{\"integerList\":" + java.util.Arrays.toString(seeds) + "}");
        CachedHotbits reader = new CachedHotbits(directory);
        for (int seed : seeds) {
            assertEquals(new Random(seed).nextInt(1001), reader.nextInt(1001));
            assertFalse(reader.isUsingSecureRandom());
        }
        assertEquals(seeds.length, reader.integersUsed());
    }

    @Test
    void emptyCacheUsesSecureRandomImmediatelyWithinBounds() throws Exception {
        CachedHotbits reader = new CachedHotbits(directory);
        for (int index = 0; index < 100; index++) {
            int value = reader.nextInt(13);
            assertTrue(value >= 0 && value < 13);
        }
        assertTrue(reader.isUsingSecureRandom());
        assertEquals(0, reader.integersUsed());
    }

    @Test
    void freshPackagesResumeSeededDrawsAndClearTheWarning() throws Exception {
        CachedHotbits reader = new CachedHotbits(directory);
        reader.nextInt(100);
        assertTrue(reader.isUsingSecureRandom());
        new HotbitsPackageStore(directory).write(new int[]{42}, HotbitsBackend.Kind.ESP32);
        Thread.sleep(150);
        assertEquals(new Random(42).nextInt(100), reader.nextInt(100));
        assertFalse(reader.isUsingSecureRandom());
    }

    @Test
    void cancellationIsNotConvertedIntoFallback() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedIOException.class, () -> new CachedHotbits(directory).nextInt(100));
        } finally {
            Thread.interrupted();
        }
    }
}
