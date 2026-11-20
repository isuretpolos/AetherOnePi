package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.ScreenUtils;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit real-window/tray check. Its temporary system-tray icon is always removed. */
public final class DesktopIconsSmokeTest {

    private DesktopIconsSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        var image = DesktopIcons.load();
        require(image.getWidth() > 0 && image.getHeight() == image.getWidth(), "Original AOPi icon did not load.");
        var configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("AOPi original icon check");
        configuration.setWindowedMode(640, 360);
        configuration.setForegroundFPS(30);
        DesktopIcons.configure(configuration);
        AtomicBoolean stopped = new AtomicBoolean();
        try (DesktopTray tray = new DesktopTray()) {
            new Lwjgl3Application(new ApplicationAdapter() {
                private int frames;
                private boolean stopInvoked;

                @Override
                public void create() {
                    tray.install(() -> { }, () -> stopped.set(true), () -> Gdx.app.exit());
                }

                @Override
                public void render() {
                    ScreenUtils.clear(0.08f, 0.05f, 0.12f, 1);
                    if (++frames > 150) {
                        throw new AssertionError("Tray icon installation/action timed out.");
                    }
                    if (!SystemTray.isSupported()) {
                        Gdx.app.exit();
                        return;
                    }
                    if (tray.isInstalled() && !stopInvoked) {
                        stopInvoked = true;
                        invokeMenu(tray, 2);
                    } else if (stopped.get()) {
                        invokeMenu(tray, 4);
                    }
                }
            }, configuration);
            require(!SystemTray.isSupported() || stopped.get(), "Tray STOP ALL did not reach the app thread.");
            tray.close();
            require(!tray.isInstalled(), "Tray icon leaked after shutdown.");
        }
        System.out.println("Original AOPi icons passed: bundled asset, native window startup, tray install, STOP ALL, Exit, and cleanup.");
    }

    private static void invokeMenu(DesktopTray tray, int index) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                try {
                    var field = DesktopTray.class.getDeclaredField("icon");
                    field.setAccessible(true);
                    TrayIcon icon = (TrayIcon) field.get(tray);
                    var item = icon.getPopupMenu().getItem(index);
                    for (var listener : item.getActionListeners()) {
                        listener.actionPerformed(new ActionEvent(item, ActionEvent.ACTION_PERFORMED, item.getLabel()));
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
