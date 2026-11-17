package de.isuret.polos.aopi.hotbits;

import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class HotbitsAcquisitionTest {

    private static final HotbitsBackend.Device DEVICE = new HotbitsBackend.Device(HotbitsBackend.Kind.ESP32,
            "test-port", "Test serial device");

    @TempDir
    Path directory;

    @Test
    void acquisitionWritesExactCompletePackagesAndStopsAtCapacity() throws Exception {
        TestSource source = new TestSource();
        HotbitsPackageStore store = new HotbitsPackageStore(directory);
        try (HotbitsAcquisition service = service(source, store, 8, 2)) {
            assertTrue(service.start(DEVICE));
            awaitIdle(service);
            assertEquals(HotbitsAcquisition.State.COMPLETE, service.snapshot().state());
            assertEquals(2, service.snapshot().sessionPackages());
            assertEquals(2, store.count());
            assertTrue(source.closed.get());
            try (var files = Files.list(directory)) {
                for (Path file : files.toList()) {
                    var json = new JsonReader().parse(Files.readString(file));
                    assertEquals(8, json.get("integerList").size);
                    for (var value : json.get("integerList")) {
                        assertTrue(value.asInt() >= 0);
                    }
                }
            }
        }
    }

    @Test
    void fullCacheDoesNotOpenADeviceOrOverwritePackages() throws Exception {
        HotbitsPackageStore store = new HotbitsPackageStore(directory);
        Path original = store.write(new int[]{42}, HotbitsBackend.Kind.ESP32);
        TestSource source = new TestSource();
        try (HotbitsAcquisition service = service(source, store, 8, 1)) {
            service.start(DEVICE);
            awaitIdle(service);
            assertFalse(source.opened);
            assertEquals(1, store.count());
            assertTrue(Files.readString(original).contains("42"));
        }
    }

    @Test
    void stopCancelsBlockedReadDiscardsPartialPackageAndRejectsDoubleStart() throws Exception {
        CountDownLatch reading = new CountDownLatch(1);
        TestSource source = new TestSource() {
            private int reads;

            @Override
            public byte[] readSample() throws Exception {
                if (reads++ == 0) {
                    return super.readSample();
                }
                reading.countDown();
                new CountDownLatch(1).await();
                return new byte[0];
            }
        };
        HotbitsPackageStore store = new HotbitsPackageStore(directory);
        try (HotbitsAcquisition service = service(source, store, 10000, 2)) {
            assertTrue(service.start(DEVICE));
            assertTrue(reading.await(2, TimeUnit.SECONDS));
            assertFalse(service.start(DEVICE));
            service.stop();
            awaitIdle(service);
            assertTrue(source.closed.get());
            assertEquals(0, store.count());
            assertEquals(0, service.snapshot().pendingIntegers());
            assertEquals(HotbitsAcquisition.State.IDLE, service.snapshot().state());
        }
    }

    @Test
    void disconnectedDeviceShowsErrorWithoutCreatingFallbackData() throws Exception {
        TestSource source = new TestSource() {
            @Override
            public byte[] readSample() throws IOException {
                throw new IOException("device disconnected");
            }
        };
        HotbitsPackageStore store = new HotbitsPackageStore(directory);
        try (HotbitsAcquisition service = service(source, store, 8, 2)) {
            service.start(DEVICE);
            awaitIdle(service);
            assertEquals(HotbitsAcquisition.State.ERROR, service.snapshot().state());
            assertTrue(service.snapshot().message().contains("disconnected"));
            assertTrue(source.closed.get());
            assertEquals(0, store.count());
        }
    }

    @Test
    void failedPackageWriteReleasesDeviceAndReportsTheFailure() throws Exception {
        TestSource source = new TestSource();
        HotbitsPackageStore store = new HotbitsPackageStore(directory) {
            @Override
            public Path write(int[] values, HotbitsBackend.Kind kind) throws IOException {
                throw new IOException("disk full");
            }
        };
        try (HotbitsAcquisition service = service(source, store, 8, 2)) {
            service.start(DEVICE);
            awaitIdle(service);
            assertEquals(HotbitsAcquisition.State.ERROR, service.snapshot().state());
            assertTrue(service.snapshot().message().contains("disk full"));
            assertEquals(0, store.count());
            assertTrue(source.closed.get());
        }
    }

    @Test
    void singlePreviewReleasesDeviceAndCreatesNoPackages() throws Exception {
        TestSource source = new TestSource();
        HotbitsPackageStore store = new HotbitsPackageStore(directory);
        try (HotbitsAcquisition service = service(source, store, 8, 2)) {
            service.preview(DEVICE);
            awaitIdle(service);
            assertArrayEquals(new byte[]{1, 2, 3}, service.snapshot().previewPng());
            assertTrue(source.closed.get());
            assertEquals(0, store.count());
        }
    }

    @Test
    void discoveryDoesNotOpenAnyDevice() throws Exception {
        TestSource source = new TestSource();
        try (HotbitsAcquisition service = service(source, new HotbitsPackageStore(directory), 8, 2)) {
            service.scan(HotbitsBackend.Kind.ESP32);
            awaitIdle(service);
            assertEquals(List.of(DEVICE), service.snapshot().devices());
            assertFalse(source.opened);
            assertEquals(HotbitsAcquisition.State.IDLE, service.snapshot().state());
        }
    }

    private HotbitsAcquisition service(TestSource source, HotbitsPackageStore store, int size, int capacity) {
        HotbitsBackend backend = new HotbitsBackend() {
            @Override
            public List<Device> discover(Kind kind) {
                return List.of(DEVICE);
            }

            @Override
            public Source create(Device device) {
                return source;
            }
        };
        return new HotbitsAcquisition(backend, store, size, capacity);
    }

    @Test
    void applicationShutdownStopsAnActiveReadAndReleasesTheDevice() throws Exception {
        CountDownLatch reading = new CountDownLatch(1);
        TestSource source = new TestSource() {
            @Override
            public byte[] readSample() throws InterruptedException {
                reading.countDown();
                new CountDownLatch(1).await();
                return new byte[0];
            }
        };
        HotbitsAcquisition service = service(source, new HotbitsPackageStore(directory), 8, 2);
        try {
            service.start(DEVICE);
            assertTrue(reading.await(2, TimeUnit.SECONDS));
        } finally {
            service.close();
        }
        assertFalse(service.isBusy());
        assertTrue(source.closed.get());
        assertFalse(service.start(DEVICE));
    }

    private void awaitIdle(HotbitsAcquisition service) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (service.isBusy() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertFalse(service.isBusy(), "Acquisition did not finish in time.");
    }

    private static class TestSource implements HotbitsBackend.Source {
        private final Random fixture = new Random(27);
        private final AtomicBoolean closed = new AtomicBoolean();
        private boolean opened;

        @Override
        public void open() {
            opened = true;
        }

        @Override
        public byte[] readSample() throws Exception {
            byte[] bytes = new byte[256];
            fixture.nextBytes(bytes);
            return bytes;
        }

        @Override
        public byte[] previewPng() {
            return new byte[]{1, 2, 3};
        }

        @Override
        public void close() {
            closed.set(true);
        }
    }
}
