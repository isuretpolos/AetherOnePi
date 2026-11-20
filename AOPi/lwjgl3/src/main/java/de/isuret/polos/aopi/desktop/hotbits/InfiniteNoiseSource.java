package de.isuret.polos.aopi.desktop.hotbits;

import de.isuret.polos.aopi.hotbits.HotbitsBackend;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns one explicitly selected USB stick, including warmup and cancellation. */
public class InfiniteNoiseSource implements HotbitsBackend.Source {

    private final InfiniteNoiseTransport transport;
    private final String serial;
    private final byte[] clocks = InfiniteNoiseDecoder.clockPattern();
    private final InfiniteNoiseDecoder decoder = new InfiniteNoiseDecoder();
    private final AtomicBoolean closed = new AtomicBoolean();
    private InfiniteNoiseTransport.Connection connection;

    public InfiniteNoiseSource(InfiniteNoiseTransport transport, String serial) {
        this.transport = transport;
        this.serial = serial;
    }

    @Override
    public synchronized void open() throws IOException {
        checkCancelled();
        connection = transport.open(serial);
        try {
            checkCancelled();
            connection.configure();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!decoder.isWarmedUp()) {
                checkCancelled();
                if (System.nanoTime() > deadline) {
                    throw new IOException("Infinite Noise warmup timed out. Check the stick and USB driver.");
                }
                decoder.decode(connection.exchange(clocks));
            }
            // Warmup data is deliberately discarded; only later samples reach package conditioning.
            checkCancelled();
        } catch (IOException | RuntimeException | LinkageError exception) {
            close();
            throw exception;
        }
    }

    @Override
    public synchronized byte[] readSample() throws IOException {
        checkCancelled();
        if (connection == null) {
            throw new IOException("Infinite Noise source is not open.");
        }
        byte[] bytes = decoder.decode(connection.exchange(clocks));
        checkCancelled();
        return bytes;
    }

    private void checkCancelled() throws IOException {
        if (closed.get() || Thread.currentThread().isInterrupted()) {
            throw new IOException("Infinite Noise operation cancelled.");
        }
    }

    @Override
    public void close() {
        // Set before taking the lock so an in-progress warmup sees cancellation between USB transfers.
        closed.set(true);
        synchronized (this) {
            if (connection != null) {
                try {
                    connection.close();
                } finally {
                    connection = null;
                }
            }
        }
    }
}
