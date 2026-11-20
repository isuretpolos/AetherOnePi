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
        configuration.setTitle(de.isuret.polos.aopi.ApplicationInfo.WINDOW_TITLE);
        DesktopIcons.configure(configuration);
        windowController.configure(configuration);
        configuration.setHdpiMode(HdpiMode.Logical);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);
        configuration.setIdleFPS(15);

        AopiApplication application = new AopiApplication(settings, repository, windowController, loadFailed,
                new DesktopHotbitsBackend(), new DesktopRateFilePicker(), new DesktopBroadcastImagePicker(), new DesktopSigilFilePicker());
        try (DesktopTray tray = new DesktopTray()) {
            configuration.setWindowListener(new com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter() {
                @Override
                public void created(com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window window) {
                    windowController.created(window);
                    tray.install(() -> {
                        if (window.isIconified()) {
                            window.restoreWindow();
                        }
                        window.focusWindow();
                    }, () -> {
                        if (application.getScreen() instanceof de.isuret.polos.aopi.ui.AopiScreen screen) {
                            screen.stopAllBroadcasts();
                        }
                    }, () -> com.badlogic.gdx.Gdx.app.exit());
                }
            });
            new Lwjgl3Application(application, configuration);
        }
    }
}
