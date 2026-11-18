package de.isuret.polos.aopi.analysis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Plain text or tab-separated lists, stored only under this application's rates folder. */
public class RateRepository {

    public record Rate(String name, String url, String classification, String subfamily) {
    }

    public record ListInfo(String id, String name) {
    }

    private final Path directory;

    public RateRepository(Path directory) {
        this.directory = directory;
    }

    public void ensureStarter(String content) throws IOException {
        if (list().isEmpty()) {
            Files.createDirectories(directory);
            Path path = directory.resolve("ANALYSIS_TIME.txt");
            if (!Files.exists(path)) {
                Files.writeString(path, content, StandardCharsets.UTF_8);
            }
        }
    }

    public List<ListInfo> list() throws IOException {
        List<ListInfo> results = new ArrayList<>();
        if (Files.isDirectory(directory)) {
            try (var files = Files.walk(directory)) {
                files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                        .filter(this::isRateFile).forEach(path -> {
                            String relative = directory.relativize(path).toString();
                            results.add(new ListInfo(relative, relative.replace('\\', '/')));
                        });
            }
        }
        results.sort(Comparator.comparing(ListInfo::name, String.CASE_INSENSITIVE_ORDER));
        return results;
    }

    public List<Rate> read(String id) throws IOException {
        Path path = directory.resolve(id).normalize();
        if (!path.toAbsolutePath().startsWith(directory.toAbsolutePath().normalize()) || !isRateFile(path)) {
            throw new IOException("Invalid rate-list path.");
        }
        return readFile(path);
    }

    /** Restore only a list that still belongs to the app's rates directory. */
    public String findSelection(String preferred) throws IOException {
        List<ListInfo> available = list();
        String normalized = preferred == null ? "" : preferred.replace('\\', '/');
        return available.stream().filter(info -> info.id().replace('\\', '/').equals(normalized))
                .map(ListInfo::id).findFirst().orElse(available.isEmpty() ? null : available.getFirst().id());
    }

    public ListInfo importFile(Path source) throws IOException {
        if (!isRateFile(source)) {
            throw new IOException("Choose a .txt or .tsv rate file.");
        }
        readFile(source);
        Files.createDirectories(directory);
        String name = source.getFileName().toString();
        Path destination = directory.resolve(name);
        if (Files.exists(destination)) {
            destination = directory.resolve(UUID.randomUUID() + "-" + name);
        }
        Files.copy(source, destination);
        return new ListInfo(destination.getFileName().toString(), destination.getFileName().toString());
    }

    private boolean isRateFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return (name.endsWith(".txt") || name.endsWith(".tsv")) && !name.startsWith("function_");
    }

    private List<Rate> readFile(Path file) throws IOException {
        if (Files.size(file) > 10 * 1024 * 1024) {
            throw new IOException("Rate files must be at most 10 MB.");
        }
        List<Rate> rates = parse(Files.readString(file, StandardCharsets.UTF_8));
        if (rates.isEmpty()) {
            throw new IOException("This rate list has no usable entries.");
        }
        return rates;
    }

    public static List<Rate> parse(String text) {
        LinkedHashMap<String, Rate> unique = new LinkedHashMap<>();
        for (String raw : text.replace("\uFEFF", "").split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length >= 2 && fields[0].equalsIgnoreCase("NAME") && fields[1].equalsIgnoreCase("URL")) {
                continue;
            }
            String name = fields[0].strip();
            if (!name.isEmpty()) {
                unique.putIfAbsent(name.toLowerCase(Locale.ROOT), new Rate(name, field(fields, 1), field(fields, 2),
                        field(fields, 3)));
            }
        }
        return List.copyOf(unique.values());
    }

    private static String field(String[] fields, int index) {
        return fields.length > index ? fields[index].strip() : "";
    }
}
