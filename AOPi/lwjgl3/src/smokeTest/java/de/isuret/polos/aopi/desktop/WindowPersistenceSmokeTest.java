package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.files.FileHandle;
import de.isuret.polos.aopi.AopiApplication;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.Setting;
import de.isuret.polos.aopi.settings.SettingsRepository;
import de.isuret.polos.aopi.settings.WindowPlacement;

import java.nio.file.Files;
import java.nio.file.Path;

/** Optional real desktop check. It uses isolated data and briefly opens three application windows. */
public final class WindowPersistenceSmokeTest {

    private static WindowPlacement expected;

    private WindowPersistenceSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("lwjgl3/build"), "window-persistence-");
        System.setProperty("aopi.dataDirectory", directory.toAbsolutePath().toString());
        for (int phase = 1; phase <= 3; phase++) {
            runPhase(phase, directory);
        }
        System.out.println("Window persistence smoke test passed: normal bounds, fullscreen restart, and return to window.");
    }

    private static void runPhase(int phase, Path directory) {
        SettingsRepository repository = new SettingsRepository(new FileHandle(directory.toFile()));
        ApplicationSettings settings = repository.load();
        settings.setFontSize(24);
        settings.setEnabled(Setting.ESP32, true);
        DesktopWindowController controller = new DesktopWindowController(settings);
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("AOPi window persistence test " + phase);
        configuration.setForegroundFPS(30);
        controller.configure(configuration);
        WindowPlacement initial = settings.getWindowPlacement();
        int width = Math.min(900, initial.width());
        int height = Math.min(640, initial.height());
        int x = initial.x() + (initial.width() - width) / 2;
        int y = initial.y() + (initial.height() - height) / 2;

        new Lwjgl3Application(new AopiApplication(settings, repository, controller, false) {
            private int frames;

            @Override
            public void render() {
                super.render();
                frames++;
                if (frames == 5) {
                    if (phase == 1) {
                        Gdx.graphics.setWindowedMode(width, height);
                        ((Lwjgl3Graphics) Gdx.graphics).getWindow().setPosition(x, y);
                    } else if (phase == 2) {
                        assertBounds(settings.getWindowPlacement(), expected);
                        require(controller.toggle(), "Entering fullscreen failed.");
                    } else {
                        require(Gdx.graphics.isFullscreen(), "Fullscreen was not restored on startup.");
                        require(controller.restoreWindow(), "Returning to the saved window failed.");
                    }
                }
                if (frames == 16) {
                    controller.capture();
                    if (phase == 1) {
                        expected = new WindowPlacement(x, y, width, height, false, false);
                        assertBounds(settings.getWindowPlacement(), expected);
                    } else if (phase == 2) {
                        require(settings.getWindowPlacement().fullscreen(), "Fullscreen was not captured.");
                    } else {
                        assertBounds(settings.getWindowPlacement(), expected);
                    }
                    Gdx.app.exit();
                }
            }
        }, configuration);

        ApplicationSettings saved = new SettingsRepository(new FileHandle(directory.toFile())).load();
        assertBounds(saved.getWindowPlacement(), expected);
        require(saved.getWindowPlacement().fullscreen() == (phase == 2), "Wrong saved fullscreen state.");
        require(saved.getFontSize() == 24 && saved.isEnabled(Setting.ESP32), "Other settings were lost.");
    }

    private static void assertBounds(WindowPlacement actual, WindowPlacement expected) {
        require(actual != null && actual.x() == expected.x() && actual.y() == expected.y()
                && actual.width() == expected.width() && actual.height() == expected.height(),
                "Bounds differ: " + actual + ", expected " + expected);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
