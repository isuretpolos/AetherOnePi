package de.isuret.polos.aopi.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Lists and reads UTF-8 text files stored under the application's texts folder. */
public class TextFileRepository {

    private static final long MAX_FILE_SIZE = 1024 * 1024;
    private static final int MAX_CHARACTERS = 100_000;

    public record TextFile(String id, String name) {
    }

    private final Path directory;

    public TextFileRepository(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    public List<TextFile> list() throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (var files = Files.walk(directory)) {
            return files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(this::isTextFile)
                    .map(path -> {
                        String id = directory.relativize(path).toString();
                        String name = id.replace('\\', '/');
                        return new TextFile(id, name);
                    })
                    .sorted(Comparator.comparing(TextFile::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
    }

    public String read(String id) throws IOException {
        Path path = directory.resolve(id).normalize();
        if (!path.startsWith(directory) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || !isTextFile(path)) {
            throw new IOException("Invalid text-file path.");
        }
        if (Files.size(path) > MAX_FILE_SIZE) {
            throw new IOException("Text files must be at most 1 MB.");
        }
        String text = Files.readString(path, StandardCharsets.UTF_8).replace("\uFEFF", "");
        if (text.length() > MAX_CHARACTERS) {
            throw new IOException("Text files must contain at most 100,000 characters.");
        }
        return text;
    }

    private boolean isTextFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".text");
    }
}
