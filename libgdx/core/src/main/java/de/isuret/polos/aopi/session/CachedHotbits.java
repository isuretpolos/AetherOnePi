package de.isuret.polos.aopi.session;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.security.SecureRandom;

/** Claims cached packages once; consumed files are archived rather than reused or deleted. */
public class CachedHotbits {

    private final Path directory;
    private int[] values = new int[0];
    private int position;
    private long integersUsed;
    private final SecureRandom secureRandom = new SecureRandom();
    private volatile boolean usingSecureRandom;
    private long nextCacheCheck = System.nanoTime();
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

    /** AOPi semantics: consume one cached integer only as the seed for one java.util.Random draw. */
    public synchronized int nextInt(int bound) throws IOException {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive.");
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new java.io.InterruptedIOException("Random draw cancelled.");
        }
        if (position == values.length) {
            // Avoid rescanning an empty/unreadable disk cache for every SecureRandom draw.
            // This only throttles file discovery; it never reuses seeds or random bits.
            if (System.nanoTime() >= nextCacheCheck) {
                try {
                    loadPackage();
                } catch (IOException | RuntimeException exception) {
                    nextCacheCheck = System.nanoTime() + 100_000_000L;
                    usingSecureRandom = true;
                    return secureRandom.nextInt(bound);
                }
            }
            if (position == values.length) {
                usingSecureRandom = true;
                return secureRandom.nextInt(bound);
            }
        }
        int seed = values[position++];
        integersUsed++;
        usingSecureRandom = false;
        return new Random(seed).nextInt(bound);
    }

    public boolean isUsingSecureRandom() {
        return usingSecureRandom;
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
            if (!value.isLong() || value.asLong() < Integer.MIN_VALUE || value.asLong() > Integer.MAX_VALUE) {
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
