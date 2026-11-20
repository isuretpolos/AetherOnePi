package de.isuret.polos.aopi.settings;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PanelAppearanceTest {

    @TempDir
    Path directory;

    @Test
    void backgroundColorAndTransparencySurviveRestart() throws Exception {
        ApplicationSettings settings = new ApplicationSettings();
        settings.setPanelColor("#1aB2c3");
        settings.setPanelTransparency(63);
        settings.setFontSize(24);
        repository().save(settings);
        ApplicationSettings loaded = repository().load();
        assertEquals("1AB2C3", loaded.getPanelColor());
        assertEquals(63, loaded.getPanelTransparency());
        assertEquals(24, loaded.getFontSize());
    }

    @Test
    void oldSettingsKeepThePreviousVisibleIndigoByDefault() throws Exception {
        Files.writeString(directory.resolve("settings.json"), "{\"fontSize\":20}");
        var settings = repository().load();
        assertEquals(ApplicationSettings.DEFAULT_PANEL_COLOR, settings.getPanelColor());
        assertEquals(0, settings.getPanelTransparency());
    }

    @Test
    void invalidManualAppearanceDoesNotDiscardOtherValues() throws Exception {
        Files.writeString(directory.resolve("settings.json"),
                "{\"fontSize\":24,\"appearance\":{\"panelColor\":\"invalid\",\"panelTransparency\":10000}}");
        var settings = repository().load();
        assertEquals(ApplicationSettings.DEFAULT_PANEL_COLOR, settings.getPanelColor());
        assertEquals(100, settings.getPanelTransparency());
        assertEquals(24, settings.getFontSize());
        settings.setPanelTransparency(-20);
        assertEquals(0, settings.getPanelTransparency());
        assertThrows(IllegalArgumentException.class, () -> settings.setPanelColor("#12345"));
    }

    @Test
    void unknownAppearanceKeysArePreserved() throws Exception {
        Files.writeString(directory.resolve("settings.json"), "{\"appearance\":{\"future\":42}}");
        var repository = repository();
        repository.save(repository.load());
        var json = new JsonReader().parse(Files.readString(directory.resolve("settings.json")));
        assertEquals(42, json.get("appearance").getInt("future"));
    }

    private SettingsRepository repository() {
        return new SettingsRepository(new FileHandle(directory.toFile()));
    }
}
