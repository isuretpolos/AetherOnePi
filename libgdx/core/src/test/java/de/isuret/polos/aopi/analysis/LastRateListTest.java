package de.isuret.polos.aopi.analysis;

import com.badlogic.gdx.files.FileHandle;
import de.isuret.polos.aopi.settings.SettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LastRateListTest {

    @TempDir
    Path directory;

    @Test
    void restartRestoresTheChosenNestedListAndMissingFilesFallBackSafely() throws Exception {
        Path rates = directory.resolve("rates");
        Files.createDirectories(rates.resolve("nested"));
        Files.writeString(rates.resolve("A-default.txt"), "First");
        Files.writeString(rates.resolve("nested/Z-chosen.tsv"), "Chosen");
        var settingsRepository = new SettingsRepository(new FileHandle(directory.toFile()));
        var settings = settingsRepository.load();
        settings.setLastRateList("nested\\Z-chosen.tsv");
        settingsRepository.save(settings);

        var restored = new SettingsRepository(new FileHandle(directory.toFile())).load();
        var lists = new RateRepository(rates);
        assertEquals("nested/Z-chosen.tsv", restored.getLastRateList());
        assertEquals("nested/Z-chosen.tsv", lists.findSelection(restored.getLastRateList()).replace('\\', '/'));
        Files.delete(rates.resolve("nested/Z-chosen.tsv"));
        assertEquals("A-default.txt", lists.findSelection(restored.getLastRateList()));
        assertEquals("A-default.txt", lists.findSelection("../outside.txt"));
        Files.delete(rates.resolve("A-default.txt"));
        assertNull(lists.findSelection(restored.getLastRateList()));
    }
}
