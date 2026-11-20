package de.isuret.polos.aopi.hotbits;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.UUID;
import java.util.stream.Stream;

/** Only complete packages become visible as JSON; existing packages are never overwritten or deleted. */
public class HotbitsPackageStore {

    private final Path directory;
    private volatile int knownCount;

    public HotbitsPackageStore(Path directory) {
        this.directory = directory;
    }

    public int count() throws IOException {
        if (!Files.exists(directory)) {
            knownCount = 0;
            return 0;
        }
        try (Stream<Path> files = Files.list(directory)) {
            knownCount = Math.toIntExact(files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("hotbits_"))
                    .filter(path -> path.getFileName().toString().endsWith(".json")).count());
            return knownCount;
        }
    }

    public int knownCount() {
        return knownCount;
    }

    public Path write(int[] values, HotbitsBackend.Kind kind) throws IOException {
        if (values.length == 0) {
            throw new IllegalArgumentException("An empty package cannot be saved.");
        }
        StringBuilder json = new StringBuilder("{\"integerList\":[");
        for (int index = 0; index < values.length; index++) {
            if (values[index] < 0) {
                throw new IllegalArgumentException("Hotbits integers must not be negative.");
            }
            if (index > 0) {
                json.append(',');
            }
            json.append(values[index]);
        }
        json.append("]}");
        Files.createDirectories(directory);
        String name = "hotbits_" + kind.name().toLowerCase(java.util.Locale.ROOT) + "_"
                + System.currentTimeMillis() + "_" + UUID.randomUUID();
        Path temporary = directory.resolve(name + ".tmp");
        Path target = directory.resolve(name + ".json");
        try {
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return target;
    }
}
