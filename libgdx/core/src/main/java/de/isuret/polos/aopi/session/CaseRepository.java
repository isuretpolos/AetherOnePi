package de.isuret.polos.aopi.session;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Reads and writes only the independent application's cases and draft recovery file. */
public class CaseRepository {

    private final Path directory;

    public CaseRepository(Path directory) {
        this.directory = directory;
    }

    public record Listing(List<CaseData> cases, List<String> unreadableFiles) {
    }

    public CaseData load(String id) throws IOException {
        Path path = casePath(id);
        CaseData result = readCase(new JsonReader().parse(Files.readString(path)), false);
        if (!result.id().equals(id)) {
            throw new IOException("The case ID does not match its filename.");
        }
        return result;
    }

    public void save(CaseData data) throws IOException {
        if (data.name().isBlank()) {
            throw new IllegalArgumentException("Enter a case name before saving.");
        }
        JsonFiles.write(casePath(data.id()), writeCase(data));
    }

    public Listing list() throws IOException {
        Path casesDirectory = directory.resolve("cases");
        List<CaseData> cases = new ArrayList<>();
        List<String> unreadable = new ArrayList<>();
        if (Files.isDirectory(casesDirectory)) {
            try (var files = Files.list(casesDirectory)) {
                for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .filter(Files::isRegularFile).toList()) {
                    String name = file.getFileName().toString();
                    try {
                        cases.add(load(name.substring(0, name.length() - 5)));
                    } catch (IOException | RuntimeException exception) {
                        unreadable.add(name);
                    }
                }
            }
        }
        cases.sort(Comparator.comparing(CaseData::lastChange).reversed());
        return new Listing(List.copyOf(cases), List.copyOf(unreadable));
    }

    public void saveDraft(CaseData.Draft draft) throws IOException {
        JsonValue root = object();
        root.addChild("case", writeCase(draft.caseData()));
        root.addChild("intention", new JsonValue(draft.intention()));
        root.addChild("notes", new JsonValue(draft.notes()));
        JsonFiles.write(directory.resolve("session-draft.json"), root);
    }

    public CaseData.Draft loadDraft() throws IOException {
        Path file = directory.resolve("session-draft.json");
        if (!Files.exists(file)) {
            return null;
        }
        JsonValue root = new JsonReader().parse(Files.readString(file));
        return new CaseData.Draft(readCase(root.require("case"), true), root.getString("intention", ""),
                root.getString("notes", ""));
    }

    public void clearDraft() throws IOException {
        Files.deleteIfExists(directory.resolve("session-draft.json"));
    }

    public void remember(String id) throws IOException {
        UUID.fromString(id);
        Path path = directory.resolve("dashboard.json");
        JsonValue root = Files.exists(path) ? new JsonReader().parse(Files.readString(path)) : object();
        if (!root.isObject()) {
            throw new IOException("Dashboard data must be a JSON object.");
        }
        List<String> ids = new ArrayList<>();
        ids.add(id);
        JsonValue previous = root.get("recentCases");
        if (previous != null && previous.isArray()) {
            for (JsonValue item : previous) {
                if (item.isString() && !ids.contains(item.asString()) && ids.size() < 10) {
                    ids.add(item.asString());
                }
            }
        }
        JsonValue recent = new JsonValue(JsonValue.ValueType.array);
        ids.forEach(value -> recent.addChild(new JsonValue(value)));
        root.remove("recentCases");
        root.addChild("recentCases", recent);
        JsonFiles.write(path, root);
    }

    private Path casePath(String id) {
        String normalized = UUID.fromString(id).toString();
        if (!normalized.equals(id)) {
            throw new IllegalArgumentException("Invalid case ID.");
        }
        return directory.resolve("cases").resolve(id + ".json");
    }

    private CaseData readCase(JsonValue root, boolean draft) {
        if (!root.isObject() || root.getInt("schemaVersion", 0) != 1) {
            throw new IllegalArgumentException("Unsupported case format. Original AOPi files are not imported automatically.");
        }
        String name = root.getString("name");
        if (!draft && name.isBlank()) {
            throw new IllegalArgumentException("The saved case has no name.");
        }
        List<CaseData.Entry> entries = new ArrayList<>();
        JsonValue sessions = root.require("sessionList");
        if (!sessions.isArray()) {
            throw new IllegalArgumentException("Session history must be an array.");
        }
        for (JsonValue entry : sessions) {
            entries.add(new CaseData.Entry(entry.getString("id"), Instant.parse(entry.getString("created")),
                    entry.getString("intention", ""), entry.getString("description", "")));
        }
        return new CaseData(root.getString("id"), name, root.getString("description", ""),
                Instant.parse(root.getString("created")), Instant.parse(root.getString("lastChange")), entries);
    }

    private JsonValue writeCase(CaseData data) {
        JsonValue root = object();
        root.addChild("schemaVersion", new JsonValue(1));
        root.addChild("id", new JsonValue(data.id()));
        root.addChild("name", new JsonValue(data.name()));
        root.addChild("description", new JsonValue(data.description()));
        root.addChild("created", new JsonValue(data.created().toString()));
        root.addChild("lastChange", new JsonValue(data.lastChange().toString()));
        JsonValue entries = new JsonValue(JsonValue.ValueType.array);
        for (CaseData.Entry entry : data.sessionList()) {
            JsonValue value = object();
            value.addChild("id", new JsonValue(entry.id()));
            value.addChild("created", new JsonValue(entry.created().toString()));
            value.addChild("intention", new JsonValue(entry.intention()));
            value.addChild("description", new JsonValue(entry.description()));
            entries.addChild(value);
        }
        root.addChild("sessionList", entries);
        return root;
    }

    private JsonValue object() {
        return new JsonValue(JsonValue.ValueType.object);
    }
}
