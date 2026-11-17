package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.files.FileHandle;
import de.isuret.polos.aopi.AopiApplication;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.SettingsRepository;
import de.isuret.polos.aopi.desktop.hotbits.DesktopHotbitsBackend;

/**
 * Creates the desktop window. No original AOPi classes or configuration are loaded.
 */
public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        FileHandle dataDirectory = new FileHandle(System.getProperty("aopi.dataDirectory", "aopi-libgdx-data"));
        SettingsRepository repository = new SettingsRepository(dataDirectory);
        ApplicationSettings settings;
        boolean loadFailed = false;
        try {
            settings = repository.load();
        } catch (RuntimeException exception) {
            settings = new ApplicationSettings();
            loadFailed = true;
            System.err.println("Cannot read settings.json; using defaults: " + exception.getMessage());
        }
        DesktopWindowController windowController = new DesktopWindowController(settings);
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("AOPi - libGDX");
        windowController.configure(configuration);
        configuration.setHdpiMode(HdpiMode.Logical);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);
        configuration.setIdleFPS(15);

        new Lwjgl3Application(new AopiApplication(settings, repository, windowController, loadFailed,
                new DesktopHotbitsBackend()), configuration);
    }
}
