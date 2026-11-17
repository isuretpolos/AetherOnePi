package de.isuret.polos.aopi.session;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Claims cached packages once; consumed files are archived rather than reused or deleted. */
public class CachedHotbits {

    private final Path directory;
    private int[] values = new int[0];
    private int position;

    public CachedHotbits(Path directory) {
        this.directory = directory;
    }

    public synchronized int nextInt(int bound) throws IOException {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive.");
        }
        long limit = (1L << 31) - (1L << 31) % bound;
        for (int attempt = 0; attempt < 10000; attempt++) {
            if (position == values.length) {
                loadPackage();
            }
            int value = values[position++];
            if (value < limit) {
                return value % bound;
            }
        }
        throw new IOException("Cached hotbits could not supply a bounded draw.");
    }

    private void loadPackage() throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("No cached hotbits. Acquire a package on the Hotbits tab first.");
        }
        List<Path> files;
        try (var stream = Files.list(directory)) {
            files = stream.filter(path -> Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().startsWith("hotbits_"))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
        }
        if (files.isEmpty()) {
            throw new IOException("No cached hotbits. Acquire a package on the Hotbits tab first.");
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
    }
}
