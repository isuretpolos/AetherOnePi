package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.isuret.polos.aopi.AopiApplication;
import de.isuret.polos.aopi.hotbits.HotbitsBackend;
import de.isuret.polos.aopi.hotbits.HotbitsPackageStore;
import de.isuret.polos.aopi.session.CaseData;
import de.isuret.polos.aopi.session.CaseRepository;
import de.isuret.polos.aopi.settings.SettingsRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/** Exercises case editing and recovery using real widgets, an isolated cache, and no physical hardware. */
public final class SessionUiSmokeTest {

    private static String savedId;

    private SessionUiSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("lwjgl3/build"), "session-ui-");
        System.setProperty("aopi.dataDirectory", directory.toAbsolutePath().toString());
        int[] fixture = new Random(42).ints(10000, 0, Integer.MAX_VALUE).toArray();
        new HotbitsPackageStore(directory.resolve("hotbits")).write(fixture, HotbitsBackend.Kind.ESP32);
        run(directory, false);
        if (new CaseRepository(directory).loadDraft() == null) {
            throw new AssertionError("Unsaved work was not preserved on close.");
        }
        run(directory, true);
        if (new CaseRepository(directory).loadDraft() != null) {
            throw new AssertionError("Saved recovery draft was not cleared.");
        }
        System.out.println("Session UI passed: save/load, rename, notes, questions, recent cases, font changes, and close/recover/save.");
    }

    private static void run(Path directory, boolean recovering) {
        SettingsRepository settingsRepository = new SettingsRepository(new FileHandle(directory.toFile()));
        var settings = settingsRepository.load();
        CaseRepository cases = new CaseRepository(directory);
        DesktopWindowController controller = new DesktopWindowController(settings);
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("AOPi Session UI test");
        configuration.setForegroundFPS(30);
        controller.configure(configuration);

        new Lwjgl3Application(new AopiApplication(settings, settingsRepository, controller, false) {
            private int frames;
            private int phase;

            @Override
            public void render() {
                super.render();
                frames++;
                if (frames > 350) {
                    throw new AssertionError("Session UI test timed out at phase " + phase);
                }
                Stage stage = (Stage) ((InputMultiplexer) Gdx.input.getInputProcessor()).getProcessors().get(1);
                try {
                    if (phase == 0 && frames > 5) {
                        click(stage, "session-tab");
                        phase++;
                    } else if (phase == 1 && ready(stage)) {
                        if (recovering) {
                            require(field(stage, "session-name").getText().equals("Recovered case edits"), "Case edits lost.");
                            require(field(stage, "session-notes").getText().equals("Unsaved recovery notes"), "Session draft lost.");
                            click(stage, "session-new");
                            require(stage.getRoot().findActor("session-unsaved-dialog") != null,
                                    "Unsaved work was not protected by a confirmation.");
                            click(stage, "session-unsaved-cancel");
                            require(field(stage, "session-notes").getText().equals("Unsaved recovery notes"),
                                    "Cancelling NEW lost the session draft.");
                            click(stage, "session-save");
                            phase = 20;
                        } else {
                            field(stage, "session-name").setText("Session smoke case");
                            field(stage, "session-description").setText("Case description\nSecond line");
                            field(stage, "session-intention").setText("First observation");
                            field(stage, "session-notes").setText("Session notes\nSecond line");
                            click(stage, "session-save");
                            phase++;
                        }
                    } else if (phase == 2 && ready(stage)) {
                        CaseData data = cases.list().cases().getFirst();
                        savedId = data.id();
                        require(data.sessionList().size() == 1, "Initial session was not saved.");
                        click(stage, "session-save");
                        phase++;
                    } else if (phase == 3 && ready(stage)) {
                        require(cases.load(savedId).sessionList().size() == 1, "Repeated SAVE added an empty session.");
                        field(stage, "session-name").setText("Renamed case");
                        click(stage, "session-save");
                        phase++;
                    } else if (phase == 4 && ready(stage)) {
                        require(cases.list().cases().size() == 1, "Renaming created a duplicate case.");
                        click(stage, "settings-tab");
                        settings.setFontSize(32);
                        phase++;
                    } else if (phase == 5) {
                        click(stage, "session-tab");
                        phase++;
                    } else if (phase == 6) {
                        require(field(stage, "session-name").getStyle().font.getCapHeight() > 16,
                                "Text-field font did not follow the global setting.");
                        NavigationLayoutSmokeTest.requireVisibleFooter(stage);
                        click(stage, "session-new");
                        phase++;
                    } else if (phase == 7 && ready(stage)) {
                        require(field(stage, "session-name").getText().isEmpty(), "NEW did not clear the editor.");
                        click(stage, "session-load");
                        phase++;
                    } else if (phase == 8 && stage.getRoot().findActor("session-load-" + savedId) != null) {
                        click(stage, "session-load-" + savedId);
                        phase++;
                    } else if (phase == 9 && ready(stage)) {
                        require(field(stage, "session-name").getText().equals("Renamed case"), "LOAD did not restore the case.");
                        click(stage, "session-questions");
                        phase++;
                    } else if (phase == 10 && ready(stage)) {
                        require(field(stage, "session-notes").getText().contains("ESSENTIAL QUESTIONS"), "Questions missing.");
                        click(stage, "session-save");
                        phase++;
                    } else if (phase == 11 && ready(stage)) {
                        require(cases.load(savedId).sessionList().size() == 2, "Question session was not appended.");
                        click(stage, "session-new");
                        phase++;
                    } else if (phase == 12 && ready(stage)) {
                        click(stage, "dashboard-tab");
                        phase++;
                    } else if (phase == 13) {
                        click(stage, "recent-case-" + savedId);
                        phase++;
                    } else if (phase == 14 && ready(stage)) {
                        require(field(stage, "session-name").getText().equals("Renamed case"), "Recent-case link failed.");
                        field(stage, "session-name").setText("Recovered case edits");
                        field(stage, "session-intention").setText("Pending observation");
                        field(stage, "session-notes").setText("Unsaved recovery notes");
                        Gdx.app.exit();
                        phase = 99;
                    } else if (phase == 20 && ready(stage)) {
                        require(cases.load(savedId).sessionList().size() == 3, "Recovered note was not saved exactly once.");
                        Gdx.app.exit();
                        phase = 99;
                    }
                } catch (java.io.IOException exception) {
                    throw new AssertionError(exception);
                }
            }
        }, configuration);
    }

    private static boolean ready(Stage stage) {
        TextButton save = stage.getRoot().findActor("session-save");
        return save != null && !save.isDisabled();
    }

    private static TextField field(Stage stage, String id) {
        TextField field = stage.getRoot().findActor(id);
        if (field == null) {
            throw new AssertionError("Missing field: " + id);
        }
        return field;
    }

    private static void click(Stage stage, String id) {
        TextButton button = stage.getRoot().findActor(id);
        require(button != null && !button.isDisabled(), "Unavailable control: " + id);
        button.fire(new ChangeListener.ChangeEvent());
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
