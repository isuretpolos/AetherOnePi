package de.isuret.polos.aopi.hotbits;

import java.io.UncheckedIOException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** One background device operation at a time. The UI only reads immutable snapshots. */
public class HotbitsAcquisition implements AutoCloseable {

    public static final int INTEGERS_PER_PACKAGE = 10000;
    public static final int MAX_PACKAGES = 20000;

    public enum State {
        IDLE, SCANNING, PREVIEWING, ACQUIRING, STOPPING, COMPLETE, ERROR;

        public boolean isBusy() {
            return this == SCANNING || this == PREVIEWING || this == ACQUIRING || this == STOPPING;
        }
    }

    public record Snapshot(State state, String message, List<HotbitsBackend.Device> devices,
                           int cachedPackages, int sessionPackages, int pendingIntegers, long sampleCount,
                           byte[] previewPng) {
    }

    private final HotbitsBackend backend;
    private final HotbitsPackageStore store;
    private final int packageSize;
    private final int capacity;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> daemon(task, "aopi-hotbits"));
    private final ExecutorService closer = Executors.newSingleThreadExecutor(task -> daemon(task, "aopi-hotbits-close"));
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile boolean stopping;
    private volatile boolean closed;
    private volatile HotbitsBackend.Source activeSource;
    private volatile HotbitsBackend.Kind acquiringKind;
    private volatile Thread workerThread;
    private volatile Snapshot snapshot = new Snapshot(State.IDLE, "Choose a source, then scan for devices.",
            List.of(), 0, 0, 0, 0, null);

    public HotbitsAcquisition(HotbitsBackend backend, HotbitsPackageStore store) {
        this(backend, store, INTEGERS_PER_PACKAGE, MAX_PACKAGES);
    }

    public HotbitsAcquisition(HotbitsBackend backend, HotbitsPackageStore store, int packageSize, int capacity) {
        if (packageSize <= 0 || capacity <= 0) {
            throw new IllegalArgumentException("Package size and capacity must be positive.");
        }
        this.backend = backend;
        this.store = store;
        this.packageSize = packageSize;
        this.capacity = capacity;
    }

    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    public Snapshot snapshot() {
        Snapshot current = snapshot;
        return new Snapshot(current.state(), current.message(), current.devices(), store.knownCount(),
                current.sessionPackages(), current.pendingIntegers(), current.sampleCount(), current.previewPng());
    }

    /** Called by the cache reader on its worker thread, including during active acquisition. */
    public void cacheChanged() throws IOException {
        store.count();
    }

    public boolean isBusy() {
        return busy.get();
    }

    public HotbitsBackend.Kind acquiringKind() {
        return acquiringKind;
    }

    public boolean scan(HotbitsBackend.Kind kind) {
        return submit(State.SCANNING, "Scanning devices...", () -> {
            List<HotbitsBackend.Device> devices = List.copyOf(backend.discover(kind));
            checkStopped();
            snapshot = new Snapshot(State.IDLE, devices.isEmpty() ? "No devices found for this source."
                    : "Select a device below.", devices, store.count(), 0, 0, 0, null);
        });
    }

    public boolean refreshCache() {
        return submit(State.SCANNING, "Counting saved packages...", () -> {
            Snapshot previous = snapshot;
            snapshot = new Snapshot(State.IDLE, "Ready. Choose a source and scan for devices.", previous.devices(),
                    store.count(), 0, 0, 0, null);
        });
    }

    public boolean preview(HotbitsBackend.Device device) {
        return submit(State.PREVIEWING, "Opening camera for a single preview...", () -> {
            open(device);
            byte[] png = activeSource.previewPng();
            checkStopped();
            Snapshot previous = snapshot;
            snapshot = new Snapshot(State.IDLE, "Preview captured; camera released. Start acquisition when ready.",
                    previous.devices(), previous.cachedPackages(), 0, 0, 0, png);
        });
    }

    public boolean start(HotbitsBackend.Device device) {
        return submit(State.ACQUIRING, "Opening " + device.name() + "...", device.kind(), () -> acquire(device));
    }

    private void acquire(HotbitsBackend.Device device) throws Exception {
        int cached = store.count();
        if (cached >= capacity) {
            publish(State.COMPLETE, "Package limit reached. Existing packages have been kept.", cached, 0, 0, 0);
            return;
        }
        open(device);
        FrameEntropyExtractor extractor = new FrameEntropyExtractor();
        EntropyConditioner conditioner = new EntropyConditioner();
        PackageWriter writer = new PackageWriter(device.kind(), cached);
        long samples = 0;
        long lastProgress = 0;
        long lastEntropy = System.nanoTime();
        while (!stopping) {
            checkStopped();
            byte[] sample = activeSource.readSample();
            checkStopped();
            if (sample == null || sample.length == 0) {
                Thread.sleep(30);
                continue;
            }
            samples++;
            writer.samples = samples;
            byte[] entropy = device.kind() == HotbitsBackend.Kind.WEBCAM ? extractor.extract(sample) : sample;
            if (entropy.length > 0) {
                lastEntropy = System.nanoTime();
            }
            conditioner.accept(entropy, writer::accept);
            long now = System.nanoTime();
            if (now - lastProgress > TimeUnit.MILLISECONDS.toNanos(250)) {
                String message = now - lastEntropy > TimeUnit.SECONDS.toNanos(5)
                        ? "Waiting for changing camera samples; no software fallback is used."
                        : "Acquiring from " + device.name() + ". You can use the other tabs.";
                publish(State.ACQUIRING, message, writer.cached, writer.saved, writer.position, samples);
                lastProgress = now;
            }
        }
        throw new CancellationException();
    }

    private void open(HotbitsBackend.Device device) throws Exception {
        activeSource = backend.create(device);
        checkStopped();
        activeSource.open();
        checkStopped();
    }

    private boolean submit(State state, String message, Operation operation) {
        return submit(state, message, null, operation);
    }

    private synchronized boolean submit(State state, String message, HotbitsBackend.Kind kind, Operation operation) {
        if (closed || !busy.compareAndSet(false, true)) {
            return false;
        }
        stopping = false;
        acquiringKind = kind;
        Snapshot previous = snapshot;
        snapshot = new Snapshot(state, message, previous.devices(), previous.cachedPackages(), 0, 0, 0, null);
        worker.execute(() -> {
            workerThread = Thread.currentThread();
            try {
                checkStopped();
                operation.run();
            } catch (CapacityReached ignored) {
                // PackageWriter already published the final complete-package count.
            } catch (Exception | LinkageError exception) {
                Snapshot current = snapshot;
                if (stopping || exception instanceof InterruptedException || exception instanceof CancellationException) {
                    publish(State.IDLE, "Stopped. Completed packages kept; partial package discarded.",
                            current.cachedPackages(), current.sessionPackages(), 0, current.sampleCount());
                } else {
                    publish(State.ERROR, "Hotbits error: " + errorMessage(exception), current.cachedPackages(),
                            current.sessionPackages(), 0, current.sampleCount());
                }
            } finally {
                HotbitsBackend.Source source = activeSource;
                activeSource = null;
                try {
                    if (source != null) {
                        source.close();
                    }
                } catch (RuntimeException exception) {
                    publish(State.ERROR, "Device could not be released: " + errorMessage(exception),
                            snapshot.cachedPackages(), snapshot.sessionPackages(), 0, snapshot.sampleCount());
                } finally {
                    if (snapshot.state() == State.STOPPING) {
                        publish(State.IDLE, "Stopped. Completed packages kept; partial package discarded.",
                                snapshot.cachedPackages(), snapshot.sessionPackages(), 0, snapshot.sampleCount());
                    }
                    workerThread = null;
                    acquiringKind = null;
                    Thread.interrupted();
                    busy.set(false);
                }
            }
        });
        return true;
    }

    public synchronized void stop() {
        if (!busy.get()) {
            return;
        }
        stopping = true;
        Snapshot current = snapshot;
        publish(State.STOPPING, "Stopping and releasing the device...", current.cachedPackages(),
                current.sessionPackages(), current.pendingIntegers(), current.sampleCount());
        Thread thread = workerThread;
        if (thread != null) {
            thread.interrupt();
        }
        HotbitsBackend.Source source = activeSource;
        if (source != null) {
            // Native reads can block. Releasing the device must not block the render thread.
            closer.execute(source::close);
        }
    }

    private void checkStopped() {
        if (stopping || closed || Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }
    }

    private void publish(State state, String message, int cached, int saved, int pending, long samples) {
        snapshot = new Snapshot(state, message, snapshot.devices(), cached, saved, pending, samples, null);
    }

    private String errorMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        stop();
        closed = true;
        worker.shutdown();
        closer.shutdown();
        try {
            worker.awaitTermination(1500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private interface Operation {
        void run() throws Exception;
    }

    private static class CapacityReached extends RuntimeException {
    }

    private class PackageWriter {

        private final HotbitsBackend.Kind kind;
        private final int[] values = new int[packageSize];
        private int position;
        private int cached;
        private int saved;
        private long samples;

        private PackageWriter(HotbitsBackend.Kind kind, int cached) {
            this.kind = kind;
            this.cached = cached;
        }

        private void accept(int value) {
            checkStopped();
            values[position++] = value;
            if (position == values.length) {
                try {
                    store.write(values, kind);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
                try {
                    // Session draws may have claimed a cached package while acquisition continued.
                    cached = store.count();
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
                saved++;
                position = 0;
                publish(State.ACQUIRING, "Saved package " + saved + " this session.", cached, saved, 0,
                        samples);
                if (cached >= capacity) {
                    publish(State.COMPLETE, "Package limit reached. Acquisition finished.", cached, saved, 0,
                            samples);
                    throw new CapacityReached();
                }
            }
        }
    }
}
