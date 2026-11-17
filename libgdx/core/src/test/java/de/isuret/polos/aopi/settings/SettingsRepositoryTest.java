package de.isuret.polos.aopi.settings;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsRepositoryTest {

    @TempDir
    Path directory;

    @Test
    void freshInstallationUsesOriginalDefaultsWithoutCreatingAFile() {
        ApplicationSettings settings = repository().load();

        assertEquals(16, settings.getFontSize());
        for (Setting setting : Setting.values()) {
            assertEquals(setting.defaultValue(), settings.isEnabled(setting));
        }
        assertTrue(settings.isEnabled(Setting.SAFETY_SWITCH));
        assertFalse(settings.isEnabled(Setting.POWER_SWITCH));
        assertFalse(Files.exists(settingsFile()));
    }

    @Test
    void fontAndBooleanChangesSurviveRestart() throws IOException {
        ApplicationSettings settings = repository().load();
        settings.setFontSize(28);
        settings.setEnabled(Setting.ESP32, true);
        settings.setEnabled(Setting.BROADCAST_EMBEDDED, false);
        repository().save(settings);

        ApplicationSettings reloaded = repository().load();
        assertEquals(28, reloaded.getFontSize());
        assertTrue(reloaded.isEnabled(Setting.ESP32));
        assertFalse(reloaded.isEnabled(Setting.BROADCAST_EMBEDDED));
        assertFalse(Files.exists(directory.resolve("settings.json.tmp")));
    }

    @Test
    void unusableFontSizesAreClampedToTheVisibleSliderRange() throws IOException {
        Files.writeString(settingsFile(), "{\"fontSize\":999}");
        assertEquals(ApplicationSettings.MAX_FONT_SIZE, repository().load().getFontSize());

        Files.writeString(settingsFile(), "{\"fontSize\":-20}");
        assertEquals(ApplicationSettings.MIN_FONT_SIZE, repository().load().getFontSize());
    }

    @Test
    void newlyAddedSettingsUseDefaultsAndUnknownValuesSurviveSaving() throws IOException {
        Files.writeString(settingsFile(), """
                {
                  "futureValue": "keep me",
                  "booleans": {"future.option": true, "power.switch": true}
                }
                """);
        SettingsRepository repository = repository();
        ApplicationSettings settings = repository.load();
        assertTrue(settings.isEnabled(Setting.SAFETY_SWITCH));
        repository.save(settings);

        var saved = new JsonReader().parse(Files.readString(settingsFile()));
        assertEquals("keep me", saved.getString("futureValue"));
        assertTrue(saved.get("booleans").getBoolean("future.option"));
    }

    @Test
    void malformedSettingsAreReportedAndTheExistingFileIsNotRewritten() throws IOException {
        String malformed = "{\"booleans\":42}";
        Files.writeString(settingsFile(), malformed);

        assertThrows(IllegalArgumentException.class, () -> repository().load());
        assertEquals(malformed, Files.readString(settingsFile()));
    }

    @Test
    void failedSaveDoesNotReplaceThePreviousSettings() throws IOException {
        SettingsRepository repository = repository();
        ApplicationSettings settings = repository.load();
        repository.save(settings);
        String original = Files.readString(settingsFile());
        // A directory at the staging path simulates a write failure without OS-specific permissions.
        Files.createDirectory(directory.resolve("settings.json.tmp"));
        settings.setFontSize(30);

        assertThrows(IOException.class, () -> repository.save(settings));
        assertEquals(original, Files.readString(settingsFile()));
    }

    private SettingsRepository repository() {
        return new SettingsRepository(new FileHandle(directory.toFile()));
    }

    @Test
    void windowPlacementAndOtherSettingsSurviveTheSameSave() throws IOException {
        ApplicationSettings settings = new ApplicationSettings();
        WindowPlacement placement = new WindowPlacement(-1500, 80, 1100, 740, true, true);
        settings.setWindowPlacement(placement);
        settings.setFontSize(24);
        settings.setEnabled(Setting.ESP32, true);
        repository().save(settings);

        ApplicationSettings restored = repository().load();
        assertEquals(placement, restored.getWindowPlacement());
        assertEquals(24, restored.getFontSize());
        assertTrue(restored.isEnabled(Setting.ESP32));
    }

    @Test
    void invalidOrDeletedWindowSectionDoesNotLoseFontAndBooleanSettings() throws IOException {
        for (String window : new String[]{"null", "42", "{}", "{\"width\":-1,\"height\":700}",
                "{\"width\":\"bad\",\"height\":700}"}) {
            Files.writeString(settingsFile(), "{\"fontSize\":22,\"booleans\":{\"trng.esp32\":true},\"window\":" + window + "}");
            ApplicationSettings settings = repository().load();
            assertNull(settings.getWindowPlacement());
            assertEquals(22, settings.getFontSize());
            assertTrue(settings.isEnabled(Setting.ESP32));
        }
        Files.writeString(settingsFile(), "{\"fontSize\":22}");
        assertNull(repository().load().getWindowPlacement());
    }

    private Path settingsFile() {
        return directory.resolve("settings.json");
    }
}
