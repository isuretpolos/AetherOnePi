package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.isuret.polos.aopi.AopiApplication;
import de.isuret.polos.aopi.session.CaseRepository;
import de.isuret.polos.aopi.settings.Setting;
import de.isuret.polos.aopi.settings.SettingsRepository;
import de.isuret.polos.aopi.ui.StatusLight;

import java.nio.file.Files;
import java.nio.file.Path;

/** Real-window workflow with isolated data and no device or hotbits access. */
public final class BroadcastUiSmokeTest {

    private BroadcastUiSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("lwjgl3/build"), "broadcast-ui-");
        System.setProperty("aopi.dataDirectory", directory.toAbsolutePath().toString());
        SettingsRepository repository = new SettingsRepository(new FileHandle(directory.toFile()));
        var settings = repository.load();
        settings.setEnabled(Setting.BROADCAST_SINGLE_RATES, true);
        DesktopWindowController controller = new DesktopWindowController(settings);
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("AOPi broadcast UI check");
        configuration.setForegroundFPS(30);
        controller.configure(configuration);
        CaseRepository cases = new CaseRepository(directory);
        new Lwjgl3Application(new AopiApplication(settings, repository, controller, false) {
            private int frames;
            private int phase;
            private String caseId;
            private int previewFrame;

            @Override
            public void render() {
                super.render();
                if (++frames > 600) {
                    throw new AssertionError("Broadcast UI timed out at phase " + phase);
                }
                Stage stage = (Stage) ((InputMultiplexer) Gdx.input.getInputProcessor()).getProcessors().get(1);
                try {
                    if (phase == 0 && frames > 8) {
                        click(stage, "session-tab");
                        phase++;
                    } else if (phase == 1 && enabled(stage, "session-save")) {
                        field(stage, "session-name").setText("Broadcast test case");
                        click(stage, "session-save");
                        phase++;
                    } else if (phase == 2 && enabled(stage, "session-save")) {
                        caseId = cases.list().cases().getFirst().id();
                        click(stage, "broadcast-tab");
                        field(stage, "broadcast-signature").setText("Alpha\nBeta");
                        field(stage, "broadcast-seconds").setText("0");
                        click(stage, "broadcast-start");
                        require(text(stage, "broadcast-status").contains("Cannot queue"), "Invalid duration accepted.");
                        field(stage, "broadcast-seconds").setText("1");
                        click(stage, "broadcast-add");
                        phase++;
                    } else if (phase == 3 && enabled(stage, "broadcast-remove-1")) {
                        requireAlignedInputs(stage);
                        click(stage, "broadcast-remove-1");
                        click(stage, "broadcast-clear");
                        click(stage, "broadcast-add");
                        click(stage, "broadcast-start");
                        phase++;
                    } else if (phase == 4 && enabled(stage, "broadcast-stop")) {
                        require(((StatusLight) stage.getRoot().findActor("broadcast-indicator")).getState()
                                == StatusLight.State.ON, "Broadcast light stayed off.");
                        click(stage, "dashboard-tab");
                        phase++;
                    } else if (phase == 5 && reportCount(directory) == 2) {
                        require(((StatusLight) stage.getRoot().findActor("broadcast-indicator")).getState()
                                == StatusLight.State.OFF, "Broadcast did not finish in the background.");
                        click(stage, "broadcast-tab");
                        require(text(stage, "broadcast-report").contains("Beta"), "Sequential queue order failed.");
                        click(stage, "broadcast-save-case");
                        phase++;
                    } else if (phase == 6 && enabled(stage, "session-save")) {
                        require(cases.load(caseId).sessionList().size() == 1, "Report missing from case.");
                        click(stage, "broadcast-tab");
                        field(stage, "broadcast-signature").setText("Stop check");
                        field(stage, "broadcast-seconds").setText("60");
                        click(stage, "broadcast-start");
                        phase++;
                    } else if (phase == 7 && enabled(stage, "broadcast-stop")) {
                        click(stage, "broadcast-stop");
                        phase++;
                    } else if (phase == 8 && text(stage, "broadcast-report").contains("Stopped")) {
                        click(stage, "broadcast-save-case");
                        phase++;
                    } else if (phase == 9 && enabled(stage, "session-save")) {
                        require(cases.load(caseId).sessionList().size() == 2, "Stopped report not saved.");
                        click(stage, "broadcast-tab");
                        click(stage, "broadcast-save-case");
                        phase++;
                    } else if (phase == 10 && enabled(stage, "session-save")) {
                        require(cases.load(caseId).sessionList().size() == 2, "Duplicate report saved.");
                        click(stage, "broadcast-tab");
                        settings.setEnabled(Setting.BROADCAST_SINGLE_RATES, false);
                        field(stage, "broadcast-signature").setText("Sulfur\nCalcarea Carbonica\nLycopodium\nPhosphorus\nPulsatilla\nArsenicum Album\nNatrum Muriaticum\nSepia");
                        click(stage, "broadcast-start");
                        phase = 11;
                        previewFrame = frames;
                        Gdx.graphics.setWindowedMode(1440, 1000);
                    } else if (phase == 11 && frames > previewFrame + 90) {
                        require(text(stage, "broadcast-current").contains("Active: 8"), "Original eight-unit broadcast mode did not start.");
                        var pixels = PixmapScreenshot.capture();
                        com.badlogic.gdx.graphics.PixmapIO.writePNG(new FileHandle(directory.resolve("broadcast-preview.png").toFile()), pixels);
                        pixels.dispose();
                        System.out.println("Broadcast preview: " + directory.resolve("broadcast-preview.png").toAbsolutePath());
                        settings.setFontSize(32);
                        phase = 12;
                        Gdx.graphics.setWindowedMode(800, 600);
                    } else if (phase == 12 && enabled(stage, "broadcast-stop")) {
                        requireAlignedInputs(stage);
                        NavigationLayoutSmokeTest.requireVisibleFooter(stage);
                        require(field(stage, "broadcast-signature").getStyle().font.getCapHeight() > 16,
                                "Broadcast input did not follow font size.");
                        Gdx.app.exit();
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        }, configuration);
        require(reportCount(directory) == 11, "Closing the app did not save every stopped broadcast.");
        require(!Files.exists(directory.resolve("hotbits")), "Visual broadcast accessed hotbits.");
        System.out.println("Broadcast UI passed: validation, sequential queue, remove/clear, background timing, LED, reports, case save, stop, shutdown, font and footer.");
    }

    private static long reportCount(Path directory) throws Exception {
        if (!Files.isDirectory(directory.resolve("broadcasts"))) {
            return 0;
        }
        try (var files = Files.list(directory.resolve("broadcasts"))) {
            return files.filter(path -> path.toString().endsWith(".json")).count();
        }
    }

    private static boolean enabled(Stage stage, String id) {
        TextButton button = stage.getRoot().findActor(id);
        return button != null && !button.isDisabled();
    }

    private static void click(Stage stage, String id) {
        TextButton button = stage.getRoot().findActor(id);
        require(button != null && !button.isDisabled(), "Button unavailable: " + id);
        button.fire(new ChangeListener.ChangeEvent());
    }

    private static TextField field(Stage stage, String id) {
        return stage.getRoot().findActor(id);
    }

    private static String text(Stage stage, String id) {
        return ((Label) stage.getRoot().findActor(id)).getText().toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireAlignedInputs(Stage stage) {
        TextField rate = field(stage, "broadcast-signature");
        TextField seconds = field(stage, "broadcast-seconds");
        TextField multiplier = field(stage, "broadcast-intensity");
        require(rate.getParent() == seconds.getParent() && rate.getParent() == multiplier.getParent(),
                "Broadcast inputs must share one row.");
        require(Math.abs(rate.getY() - seconds.getY()) < 1 && Math.abs(rate.getY() - multiplier.getY()) < 1
                        && Math.abs(rate.getHeight() - seconds.getHeight()) < 1
                        && Math.abs(rate.getHeight() - multiplier.getHeight()) < 1,
                "Rate, Seconds, and Multiplier are not aligned.");
        require(rate.getWidth() > 80, "Compact numeric controls leave no room for the rate field.");
    }

    private static class PixmapScreenshot {
        static com.badlogic.gdx.graphics.Pixmap capture() {
            int width = Gdx.graphics.getBackBufferWidth();
            int height = Gdx.graphics.getBackBufferHeight();
            var source = com.badlogic.gdx.graphics.Pixmap.createFromFrameBuffer(0, 0, width, height);
            var image = new com.badlogic.gdx.graphics.Pixmap(width, height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            for (int y = 0; y < height; y++) {
                image.drawPixmap(source, 0, height - 1 - y, width, 1, 0, y, width, 1);
            }
            source.dispose();
            return image;
        }
    }
}
