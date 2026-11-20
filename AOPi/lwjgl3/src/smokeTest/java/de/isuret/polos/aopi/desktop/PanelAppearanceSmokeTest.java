package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.isuret.polos.aopi.AopiApplication;
import de.isuret.polos.aopi.settings.SettingsRepository;

import java.nio.file.Files;
import java.nio.file.Path;

/** Samples actual rendered pixels to verify color, alpha compositing, and preview persistence. */
public final class PanelAppearanceSmokeTest {

    private PanelAppearanceSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("lwjgl3/build"), "panel-appearance-");
        System.setProperty("aopi.dataDirectory", directory.toAbsolutePath().toString());
        var repository = new SettingsRepository(new FileHandle(directory.toFile()));
        var settings = repository.load();
        var controller = new DesktopWindowController(settings);
        var configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("AOPi panel appearance check");
        configuration.setForegroundFPS(30);
        controller.configure(configuration);
        new Lwjgl3Application(new AopiApplication(settings, repository, controller, false) {
            private int frames;
            private int phase;
            private int transparentPixel;

            @Override
            public void render() {
                super.render();
                Stage stage = (Stage) ((InputMultiplexer) Gdx.input.getInputProcessor()).getProcessors().get(1);
                if (++frames > 250) {
                    throw new AssertionError("Panel appearance timed out at phase " + phase);
                }
                if (phase == 0 && frames > 5) {
                    click(stage, "settings-tab");
                    TextField hex = stage.getRoot().findActor("panel-color");
                    hex.setText("#FF0000");
                    phase++;
                } else if (phase == 1) {
                    require(panelPixel(stage) == 0xff0000ff, "Panel color preview did not render red.");
                    Slider alpha = stage.getRoot().findActor("panel-transparency");
                    alpha.setValue(100);
                    phase++;
                } else if (phase == 2) {
                    transparentPixel = panelPixel(stage);
                    require(transparentPixel != 0xff0000ff, "An opaque backing still blocks panel transparency.");
                    Slider alpha = stage.getRoot().findActor("panel-transparency");
                    alpha.setValue(50);
                    phase++;
                } else if (phase == 3) {
                    int pixel = panelPixel(stage);
                    int expectedRed = (255 + (transparentPixel >>> 24 & 255)) / 2;
                    int expectedGreen = (transparentPixel >>> 16 & 255) / 2;
                    require(Math.abs((pixel >>> 24 & 255) - expectedRed) <= 2
                            && Math.abs((pixel >>> 16 & 255) - expectedGreen) <= 2,
                            "Transparency did not composite over the galaxy.");
                    click(stage, "panel-color-choose");
                    Slider green = stage.getRoot().findActor("panel-color-green");
                    green.setValue(128);
                    require(settings.getPanelColor().equals("FF8000"), "RGB chooser did not update the color.");
                    click(stage, "panel-color-done");
                    phase++;
                } else if (phase == 4 && frames > 40) {
                    var restored = new SettingsRepository(new FileHandle(directory.toFile())).load();
                    require(restored.getPanelColor().equals("FF8000") && restored.getPanelTransparency() == 50,
                            "Panel appearance was not automatically saved.");
                    click(stage, "dashboard-tab");
                    phase++;
                } else if (phase == 5) {
                    require(panelPixel(stage) != transparentPixel, "Panel preview did not carry over to Dashboard.");
                    NavigationLayoutSmokeTest.requireVisibleFooter(stage);
                    System.out.println("Panel appearance passed: hex/RGB live preview, transparency pixels, galaxy compositing, autosave/reload, and cross-tab rendering.");
                    Gdx.app.exit();
                }
            }
        }, configuration);
    }

    private static int panelPixel(Stage stage) {
        var panel = stage.getRoot().findActor("page-panel");
        var position = panel.localToStageCoordinates(new Vector2(6, 6));
        var image = Pixmap.createFromFrameBuffer((int) (position.x * Gdx.graphics.getBackBufferWidth() / stage.getWidth()),
                (int) (position.y * Gdx.graphics.getBackBufferHeight() / stage.getHeight()), 1, 1);
        try {
            return image.getPixel(0, 0);
        } finally {
            image.dispose();
        }
    }

    private static void click(Stage stage, String name) {
        TextButton button = stage.getRoot().findActor(name);
        button.fire(new ChangeListener.ChangeEvent());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
