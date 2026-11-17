package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.math.Vector2;
import de.isuret.polos.aopi.hotbits.HotbitsBackend;

import java.util.List;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.isuret.polos.aopi.AopiApplication;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.SettingsRepository;

import java.nio.file.Files;
import java.nio.file.Path;

/** Exercises real Scene2D layout across navigation, font changes, and window resizing. */
public final class NavigationLayoutSmokeTest {

    private NavigationLayoutSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("lwjgl3/build"), "navigation-layout-");
        System.setProperty("aopi.dataDirectory", directory.toAbsolutePath().toString());
        ApplicationSettings settings = new ApplicationSettings();
        SettingsRepository repository = new SettingsRepository(new FileHandle(directory.toFile()));
        DesktopWindowController controller = new DesktopWindowController(settings);
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("AOPi navigation layout test");
        configuration.setForegroundFPS(30);
        controller.configure(configuration);

        HotbitsBackend backend = new HotbitsBackend() {
            @Override
            public List<Device> discover(Kind kind) {
                return List.of();
            }

            @Override
            public Source create(Device device) {
                throw new UnsupportedOperationException("This layout check never opens hardware.");
            }
        };
        new Lwjgl3Application(new AopiApplication(settings, repository, controller, false, backend) {
            private int frames;
            private int scenario;
            private float dashboardWidth;
            private float dashboardHeight;

            @Override
            public void render() {
                super.render();
                frames++;
                Stage stage = (Stage) ((InputMultiplexer) Gdx.input.getInputProcessor()).getProcessors().get(1);
                TextButton dashboard = stage.getRoot().findActor("dashboard-tab");
                TextButton settingsTab = stage.getRoot().findActor("settings-tab");
                int step = frames % 12;
                if (step == 1) {
                    settings.setFontSize(new int[]{14, 24, 32, 32}[scenario]);
                    if (scenario == 3) {
                        Gdx.graphics.setWindowedMode(800, 600);
                    }
                    dashboard.fire(new ChangeListener.ChangeEvent());
                } else if (step == 5) {
                    requireVisibleFooter(stage);
                    requireFullHeight(dashboard);
                    dashboardWidth = dashboard.getWidth();
                    dashboardHeight = dashboard.getHeight();
                    settingsTab.fire(new ChangeListener.ChangeEvent());
                } else if (step == 8) {
                    requireVisibleFooter(stage);
                    requireFullHeight(dashboard);
                    requireSameSize(dashboard, dashboardWidth, dashboardHeight);
                    TextButton hotbits = stage.getRoot().findActor("hotbits-tab");
                    hotbits.fire(new ChangeListener.ChangeEvent());
                } else if (step == 10) {
                    requireVisibleFooter(stage);
                    dashboard.fire(new ChangeListener.ChangeEvent());
                } else if (step == 11) {
                    requireVisibleFooter(stage);
                    requireFullHeight(dashboard);
                    requireSameSize(dashboard, dashboardWidth, dashboardHeight);
                    scenario++;
                    if (scenario == 4) {
                        System.out.println("Navigation/footer layout passed on Dashboard, Settings, and Hotbits at 14, 24, and 32 px, including 800 x 600.");
                        Gdx.app.exit();
                    }
                }
            }
        }, configuration);
    }

    static void requireVisibleFooter(Stage stage) {
        Actor footer = stage.getRoot().findActor("service-status");
        Actor panel = stage.getRoot().findActor("page-panel");
        Vector2 footerPosition = footer.localToStageCoordinates(new Vector2());
        Vector2 panelPosition = panel.localToStageCoordinates(new Vector2());
        if (!footer.isVisible() || footer.getHeight() <= 0 || footerPosition.y < -0.5f
                || footerPosition.y + footer.getHeight() > panelPosition.y + 0.5f
                || footerPosition.y + footer.getHeight() > stage.getHeight() + 0.5f) {
            throw new AssertionError("Status footer is hidden, clipped, or covered by the page panel: footer y="
                    + footerPosition.y + ", height=" + footer.getHeight() + ", panel y=" + panelPosition.y);
        }
    }

    private static void requireFullHeight(TextButton button) {
        if (button.getHeight() + 0.5f < button.getPrefHeight()) {
            throw new AssertionError("Navigation button compressed: " + button.getHeight()
                    + ", preferred height " + button.getPrefHeight());
        }
    }

    private static void requireSameSize(TextButton button, float width, float height) {
        if (Math.abs(button.getWidth() - width) > 0.5f || Math.abs(button.getHeight() - height) > 0.5f) {
            throw new AssertionError("Navigation dimensions changed between Dashboard and Settings.");
        }
    }
}
