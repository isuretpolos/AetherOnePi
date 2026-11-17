package de.isuret.polos.aopi.session;

import de.isuret.polos.aopi.hotbits.HotbitsBackend;
import de.isuret.polos.aopi.hotbits.HotbitsPackageStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CachedHotbitsAvailabilityTest {

    @TempDir
    Path directory;

    @Test
    void activeAcquisitionCanSupplyTheNextPackageWithoutRestartingTheDraw() throws Exception {
        HotbitsPackageStore store = new HotbitsPackageStore(directory);
        CachedHotbits reader = new CachedHotbits(directory, store::count);
        AtomicBoolean waited = new AtomicBoolean();
        int value = reader.nextInt(100, () -> true, () -> {
            waited.set(true);
            try {
                store.write(new int[]{42}, HotbitsBackend.Kind.ESP32);
            } catch (java.io.IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });
        assertTrue(waited.get());
        assertEquals(42, value);
        assertEquals(0, store.knownCount());
    }

    @Test
    void emptyCacheWithoutAcquisitionFailsImmediately() {
        assertThrows(CachedHotbits.EmptyCacheException.class,
                () -> new CachedHotbits(directory).nextInt(100, () -> false,
                        () -> fail("Must not wait without an active source.")));
    }

    @Test
    void stoppingAcquisitionEndsTheWait() {
        AtomicBoolean acquiring = new AtomicBoolean(true);
        assertThrows(CachedHotbits.EmptyCacheException.class,
                () -> new CachedHotbits(directory).nextInt(100, acquiring::get, () -> acquiring.set(false)));
    }

    @Test
    void waitingForAcquisitionRemainsInterruptible() {
        try {
            assertThrows(InterruptedIOException.class,
                    () -> new CachedHotbits(directory).nextInt(100, () -> true,
                            () -> Thread.currentThread().interrupt()));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
