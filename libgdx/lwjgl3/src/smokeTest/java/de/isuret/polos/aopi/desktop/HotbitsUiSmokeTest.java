package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.isuret.polos.aopi.AopiApplication;
import de.isuret.polos.aopi.hotbits.HotbitsBackend;
import de.isuret.polos.aopi.hotbits.HotbitsPackageStore;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.Setting;
import de.isuret.polos.aopi.settings.SettingsRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Uses deterministic test input, never opens a camera or port, and writes only isolated test data. */
public final class HotbitsUiSmokeTest {

    private HotbitsUiSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("lwjgl3/build"), "hotbits-ui-");
        System.setProperty("aopi.dataDirectory", directory.toAbsolutePath().toString());
        ApplicationSettings settings = new ApplicationSettings();
        settings.setEnabled(Setting.ESP32, true);
        SettingsRepository repository = new SettingsRepository(new FileHandle(directory.toFile()));
        DesktopWindowController controller = new DesktopWindowController(settings);
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("AOPi Hotbits UI test");
        configuration.setForegroundFPS(30);
        controller.configure(configuration);
        AtomicBoolean released = new AtomicBoolean();
        AtomicInteger reads = new AtomicInteger();
        HotbitsBackend backend = new HotbitsBackend() {
            @Override
            public List<Device> discover(Kind kind) {
                return List.of(new Device(kind, "fixture", "Test input (no physical hardware)"));
            }

            @Override
            public Source create(Device device) {
                return new Source() {
                    private final Random fixture = new Random(581);

                    @Override
                    public void open() {
                        released.set(false);
                    }

                    @Override
                    public byte[] readSample() throws InterruptedException {
                        byte[] bytes = new byte[4096];
                        fixture.nextBytes(bytes);
                        reads.incrementAndGet();
                        Thread.sleep(3);
                        return bytes;
                    }

                    @Override
                    public void close() {
                        released.set(true);
                    }
                };
            }
        };

        new Lwjgl3Application(new AopiApplication(settings, repository, controller, false, backend) {
            private int frames;
            private int phase;
            private int backgroundReads;

            @Override
            public void render() {
                super.render();
                frames++;
                if (frames > 300) {
                    throw new AssertionError("Hotbits UI test timed out at phase " + phase);
                }
                Stage stage = (Stage) ((InputMultiplexer) Gdx.input.getInputProcessor()).getProcessors().get(1);
                if (phase == 0 && frames >= 5) {
                    click(stage, "hotbits-tab");
                    click(stage, "hotbits-esp32");
                    click(stage, "hotbits-scan");
                    phase++;
                } else if (phase == 1 && stage.getRoot().findActor("hotbits-device-fixture") != null) {
                    click(stage, "hotbits-device-fixture");
                    phase++;
                } else if (phase == 2 && !button(stage, "hotbits-start").isDisabled()) {
                    click(stage, "hotbits-start");
                    phase++;
                } else if (phase == 3 && reads.get() > 20) {
                    click(stage, "dashboard-tab");
                    backgroundReads = reads.get();
                    settings.setFontSize(32);
                    phase++;
                } else if (phase == 4 && reads.get() > backgroundReads + 100) {
                    click(stage, "hotbits-tab");
                    click(stage, "hotbits-stop");
                    phase++;
                } else if (phase == 5 && released.get() && !button(stage, "hotbits-start").isDisabled()) {
                    try {
                        HotbitsPackageStore store = new HotbitsPackageStore(directory.resolve("hotbits"));
                        if (store.count() == 0) {
                            throw new AssertionError("No complete package was produced.");
                        }
                        try (var files = Files.list(directory.resolve("hotbits"))) {
                            if (files.anyMatch(path -> path.toString().endsWith(".tmp"))) {
                                throw new AssertionError("Incomplete package remained after STOP.");
                            }
                        }
                    } catch (java.io.IOException exception) {
                        throw new AssertionError(exception);
                    }
                    System.out.println("Hotbits UI smoke test passed: scan, select, acquire, background navigation, live font change, stop, and packages.");
                    Gdx.app.exit();
                    phase++;
                }
            }
        }, configuration);
    }

    private static TextButton button(Stage stage, String name) {
        TextButton button = stage.getRoot().findActor(name);
        if (button == null) {
            throw new AssertionError("Missing control: " + name);
        }
        return button;
    }

    private static void click(Stage stage, String name) {
        TextButton button = button(stage, name);
        if (button.isDisabled()) {
            throw new AssertionError("Disabled control: " + name);
        }
        button.fire(new ChangeListener.ChangeEvent());
    }
}
