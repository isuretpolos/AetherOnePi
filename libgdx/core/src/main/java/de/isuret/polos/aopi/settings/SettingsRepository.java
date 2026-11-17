package de.isuret.polos.aopi.settings;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Persists settings only inside the new application's data directory. */
public class SettingsRepository {

    private final FileHandle file;
    private JsonValue document = new JsonValue(JsonValue.ValueType.object);

    public SettingsRepository(FileHandle directory) {
        file = directory.child("settings.json");
    }

    public ApplicationSettings load() {
        ApplicationSettings settings = new ApplicationSettings();
        if (!file.exists()) {
            return settings;
        }
        JsonValue parsed = new JsonReader().parse(file);
        if (!parsed.isObject()) {
            throw new IllegalArgumentException("Settings must be a JSON object.");
        }
        JsonValue booleans = parsed.get("booleans");
        if (booleans != null) {
            if (!booleans.isObject()) {
                throw new IllegalArgumentException("Settings booleans must be an object.");
            }
            for (Setting setting : Setting.values()) {
                JsonValue value = booleans.get(setting.key());
                if (value != null && value.isBoolean()) {
                    settings.setEnabled(setting, value.asBoolean());
                }
            }
        }
        JsonValue size = parsed.get("fontSize");
        if (size != null && size.isNumber()) {
            settings.setFontSize(size.asInt());
        }
        JsonValue window = parsed.get("window");
        if (window != null && window.isObject()) {
            try {
                int width = window.getInt("width", 0);
                int height = window.getInt("height", 0);
                if (width > 0 && height > 0) {
                    settings.setWindowPlacement(new WindowPlacement(window.getInt("x", 0), window.getInt("y", 0),
                            width, height, window.getBoolean("maximized", false),
                            window.getBoolean("fullscreen", false)));
                }
            } catch (RuntimeException ignored) {
                // A manually edited window section must not prevent other settings from loading.
            }
        }
        // Preserve unrecognized keys so later settings are not lost on a round trip.
        document = parsed;
        return settings;
    }

    public void save(ApplicationSettings settings) throws IOException {
        document.remove("fontSize");
        document.addChild("fontSize", new JsonValue(settings.getFontSize()));
        JsonValue booleans = document.get("booleans");
        if (booleans == null) {
            booleans = new JsonValue(JsonValue.ValueType.object);
            document.addChild("booleans", booleans);
        }
        for (Setting setting : Setting.values()) {
            booleans.remove(setting.key());
            booleans.addChild(setting.key(), new JsonValue(settings.isEnabled(setting)));
        }

        WindowPlacement placement = settings.getWindowPlacement();
        if (placement != null) {
            JsonValue window = new JsonValue(JsonValue.ValueType.object);
            window.addChild("x", new JsonValue(placement.x()));
            window.addChild("y", new JsonValue(placement.y()));
            window.addChild("width", new JsonValue(placement.width()));
            window.addChild("height", new JsonValue(placement.height()));
            window.addChild("maximized", new JsonValue(placement.maximized()));
            window.addChild("fullscreen", new JsonValue(placement.fullscreen()));
            document.remove("window");
            document.addChild("window", window);
        }

        Path target = file.file().toPath();
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, document.prettyPrint(com.badlogic.gdx.utils.JsonWriter.OutputType.json, 100),
                StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
