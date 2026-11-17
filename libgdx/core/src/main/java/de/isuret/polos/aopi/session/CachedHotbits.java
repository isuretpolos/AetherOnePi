package de.isuret.polos.aopi.session;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Claims cached packages once; consumed files are archived rather than reused or deleted. */
public class CachedHotbits {

    private final Path directory;
    private int[] values = new int[0];
    private int position;
    private long bitBuffer;
    private int bufferedBits;
    private long integersUsed;
    private final CacheChanged onCacheChanged;

    @FunctionalInterface
    public interface CacheChanged {
        void run() throws IOException;
    }

    public static class EmptyCacheException extends IOException {
        public EmptyCacheException() {
            super("No unused cached hotbits remain. Acquire more packages on the Hotbits tab.");
        }
    }

    public CachedHotbits(Path directory) {
        this(directory, () -> { });
    }

    public CachedHotbits(Path directory, CacheChanged onCacheChanged) {
        this.directory = directory;
        this.onCacheChanged = onCacheChanged;
    }

    /** Wait only while a real source is active. Never hold the reader lock while waiting. */
    public int nextInt(int bound, BooleanSupplier acquisitionActive, Runnable onWaiting) throws IOException {
        while (true) {
            try {
                return nextInt(bound);
            } catch (EmptyCacheException exception) {
                if (!acquisitionActive.getAsBoolean()) {
                    throw exception;
                }
                onWaiting.run();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new java.io.InterruptedIOException("Analysis cancelled while waiting for hotbits.");
                }
            }
        }
    }

    public synchronized int nextInt(int bound) throws IOException {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive.");
        }
        if (bound == 1) {
            return 0;
        }
        int requiredBits = Integer.SIZE - Integer.numberOfLeadingZeros(bound - 1);
        long mask = (1L << requiredBits) - 1;
        for (int attempt = 0; attempt < 10000; attempt++) {
            // Keep unused bits for the next draw, even across package boundaries.
            // At most 61 bits fit here: 30 remaining bits plus one 31-bit integer.
            while (bufferedBits < requiredBits) {
                if (position == values.length) {
                    loadPackage();
                }
                bitBuffer |= (long) values[position++] << bufferedBits;
                bufferedBits += 31;
                integersUsed++;
            }
            int value = (int) (bitBuffer & mask);
            bitBuffer >>>= requiredBits;
            bufferedBits -= requiredBits;
            // Reject the unused tail rather than introduce modulo bias.
            if (value < bound) {
                return value;
            }
        }
        throw new IOException("Cached hotbits could not supply a bounded draw.");
    }

    public synchronized long integersUsed() {
        return integersUsed;
    }

    private void loadPackage() throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new EmptyCacheException();
        }
        List<Path> files;
        try (var stream = Files.list(directory)) {
            files = stream.filter(path -> Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().startsWith("hotbits_"))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
        }
        if (files.isEmpty()) {
            onCacheChanged.run();
            throw new EmptyCacheException();
        }
        Path file = files.getFirst();
        JsonValue list = new JsonReader().parse(Files.readString(file)).require("integerList");
        if (!list.isArray() || list.size == 0) {
            throw new IOException("Invalid hotbits package: " + file.getFileName());
        }
        List<Integer> checked = new ArrayList<>();
        for (JsonValue value : list) {
            if (!value.isLong() || value.asLong() < 0 || value.asLong() > Integer.MAX_VALUE) {
                throw new IOException("Invalid hotbits integer in " + file.getFileName());
            }
            checked.add(value.asInt());
        }
        Path archive = directory.resolve("consumed");
        Files.createDirectories(archive);
        // A move without replacement claims the file exclusively, including across running app instances.
        Files.move(file, archive.resolve(file.getFileName()));
        values = checked.stream().mapToInt(Integer::intValue).toArray();
        position = 0;
        onCacheChanged.run();
    }
}
