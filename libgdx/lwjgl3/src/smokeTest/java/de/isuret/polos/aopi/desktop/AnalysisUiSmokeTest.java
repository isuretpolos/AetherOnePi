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
import de.isuret.polos.aopi.analysis.AnalysisResult;
import de.isuret.polos.aopi.analysis.AnalysisStore;
import de.isuret.polos.aopi.hotbits.HotbitsBackend;
import de.isuret.polos.aopi.hotbits.HotbitsPackageStore;
import de.isuret.polos.aopi.session.CaseRepository;
import de.isuret.polos.aopi.settings.SettingsRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/** Full analysis workflow with isolated fixture hotbits; no physical hardware or user data. */
public final class AnalysisUiSmokeTest {

    private AnalysisUiSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("lwjgl3/build"), "analysis-ui-");
        System.setProperty("aopi.dataDirectory", directory.toAbsolutePath().toString());
        Files.createDirectories(directory.resolve("rates"));
        Files.writeString(directory.resolve("rates/A-default.txt"), "Default rate");
        Files.writeString(directory.resolve("rates/fixture.tsv"),
                "NAME\tURL\tCLASSIFICATION\tSUBFAMILY\nAlpha\t\tPlants\tFamily\n"
                        + String.join("\n", java.util.stream.IntStream.range(1, 100).mapToObj(index -> "Rate " + index).toList()));
        Random fixture = new Random(456);
        HotbitsPackageStore packages = new HotbitsPackageStore(directory.resolve("hotbits"));
        for (int index = 0; index < 2; index++) {
            packages.write(fixture.ints(10000, 0, Integer.MAX_VALUE).toArray(), HotbitsBackend.Kind.ESP32);
        }
        SettingsRepository repository = new SettingsRepository(new FileHandle(directory.toFile()));
        var settings = repository.load();
        DesktopWindowController controller = new DesktopWindowController(settings);
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("AOPi analysis UI test");
        configuration.setForegroundFPS(30);
        controller.configure(configuration);
        configuration.setWindowedMode(1900, 1000);
        CaseRepository cases = new CaseRepository(directory);
        AnalysisStore store = new AnalysisStore(directory);
        new Lwjgl3Application(new AopiApplication(settings, repository, controller, false) {
            private int frames;
            private int phase;
            private String caseId;
            private AnalysisResult snapshot;

            @Override
            public void render() {
                super.render();
                if (++frames > 600) {
                    throw new AssertionError("Analysis test timed out at phase " + phase);
                }
                Stage stage = (Stage) ((InputMultiplexer) Gdx.input.getInputProcessor()).getProcessors().get(1);
                try {
                    if (phase == 0 && frames > 8) {
                        click(stage, "session-tab");
                        phase++;
                    } else if (phase == 1 && enabled(stage, "session-save")) {
                        field(stage, "session-name").setText("Analysis test case");
                        click(stage, "session-save");
                        phase++;
                    } else if (phase == 2 && enabled(stage, "session-save")) {
                        caseId = cases.list().cases().getFirst().id();
                        click(stage, "analysis-tab");
                        phase++;
                    } else if (phase == 3 && enabled(stage, "analysis-run")) {
                        click(stage, "analysis-select");
                        phase = 30;
                    } else if (phase == 30 && stage.getRoot().findActor("analysis-list-search") != null) {
                        field(stage, "analysis-list-search").setText("FIXTURE");
                        click(stage, "analysis-list-fixture.tsv");
                        phase = 31;
                    } else if (phase == 31 && enabled(stage, "analysis-run")
                            && stage.getRoot().findActor("analysis-list-dialog") == null) {
                        stage.setKeyboardFocus(field(stage, "analysis-filter"));
                        Gdx.input.getInputProcessor().keyDown(com.badlogic.gdx.Input.Keys.CONTROL_LEFT);
                        Gdx.input.getInputProcessor().keyUp(com.badlogic.gdx.Input.Keys.CONTROL_LEFT);
                        phase = 4;
                    } else if (phase == 4 && enabled(stage, "analysis-gv-next")) {
                        snapshot = store.load();
                        require(snapshot.rows().size() == 20, "Result must contain only the top 20 rates.");
                        field(stage, "analysis-filter").setText("pLaNtS");
                        long plantRows = snapshot.rows().stream().filter(row -> row.classification().equals("Plants")).count();
                        require(visibleRates(stage) == plantRows, "Metadata filter failed.");
                        field(stage, "analysis-filter").setText("no matching rate");
                        require(visibleRates(stage) == 0, "Empty filter failed.");
                        field(stage, "analysis-filter").setText("");
                        Gdx.input.getInputProcessor().keyDown(com.badlogic.gdx.Input.Keys.ENTER);
                        Gdx.input.getInputProcessor().keyUp(com.badlogic.gdx.Input.Keys.ENTER);
                        phase++;
                    } else if (phase == 5 && enabled(stage, "analysis-gv-all")) {
                        require(store.load().generalVitality() != null, "Target GV not persisted.");
                        requireNextRate(stage, "analysis-result-0");
                        click(stage, "analysis-gv-next");
                        phase = 50;
                    } else if (phase == 50 && enabled(stage, "analysis-gv-all")) {
                        require(store.load().rows().getFirst().generalVitality() != null, "First rate GV not checked.");
                        requireNextRate(stage, "analysis-result-1");
                        requireGreenOutline(stage);
                        field(stage, "analysis-filter").setText("no matching rate");
                        requireNextRate(stage, null);
                        field(stage, "analysis-filter").setText("");
                        requireNextRate(stage, "analysis-result-1");
                        click(stage, "analysis-gv-all");
                        phase = 6;
                    } else if (phase == 6 && enabled(stage, "analysis-save-case")) {
                        snapshot = store.load();
                        require(snapshot.rows().subList(0, 20).stream().allMatch(row -> row.generalVitality() != null), "GV ALL failed.");
                        requireNextRate(stage, null);
                        click(stage, "analysis-save-case");
                        phase++;
                    } else if (phase == 7 && enabled(stage, "session-save")) {
                        var entries = cases.load(caseId).sessionList();
                        require(entries.size() == 1 && snapshot.equals(entries.getFirst().analysis()), "Snapshot was not saved.");
                        click(stage, "session-view-analysis-" + snapshot.id());
                        phase++;
                    } else if (phase == 8) {
                        require(visibleRates(stage) == 20, "History did not reopen analysis.");
                        require(stage.getRoot().findActor("analysis-summary") == null, "Unwanted statistics summary remains.");
                        require(text(stage, "analysis-target-gv").contains(snapshot.generalVitality().toString()), "Target GV missing from heading.");
                        Label target = stage.getRoot().findActor("analysis-target-gv");
                        Label selected = stage.getRoot().findActor("analysis-selected");
                        require(target.getX() < selected.getX() && Math.abs(target.getTop() - selected.getTop()) < 1,
                                "Target GV and selected list must share the heading, in that order.");
                        requireCompactRows(stage);
                        requireHeaderPlacement(stage);
                        click(stage, "analysis-broadcast");
                        require(field(stage, "broadcast-signature").getText().lines().count() == 20,
                                "Analysis rates did not transfer to Broadcast.");
                        require(((TextButton) stage.getRoot().findActor("broadcast-tab")).isChecked(),
                                "Analysis BROADCAST did not open the Broadcast tab.");
                        click(stage, "analysis-tab");
                        settings.setFontSize(32);
                        // Native resizing may invoke render before setWindowedMode returns.
                        phase = 9;
                        Gdx.graphics.setWindowedMode(800, 600);
                    } else if (phase == 9) {
                        NavigationLayoutSmokeTest.requireVisibleFooter(stage);
                        require(field(stage, "analysis-filter").getStyle().font.getCapHeight() > 16, "Font did not update.");
                        requireCompactRows(stage);
                        requireHeaderPlacement(stage);
                        com.badlogic.gdx.scenes.scene2d.ui.ScrollPane columns = stage.getRoot().findActor("analysis-result-columns");
                        require(columns.getMaxX() > 0, "Wide result columns cannot scroll in a small window.");
                        click(stage, "analysis-save-case");
                        phase++;
                    } else if (phase == 10 && enabled(stage, "session-save")) {
                        require(cases.load(caseId).sessionList().size() == 1, "Duplicate snapshot saved.");
                        click(stage, "analysis-tab");
                        click(stage, "analysis-run");
                        click(stage, "analysis-stop");
                        phase++;
                    } else if (phase == 11 && enabled(stage, "analysis-run")) {
                        require(text(stage, "analysis-status").startsWith("Cancelled"), "Cancellation was not reported.");
                        require(snapshot.equals(store.load()), "Cancellation replaced the completed result.");
                        // Consume the remaining test cache, then verify exhaustion keeps the completed snapshot.
                        try (var files = Files.list(directory.resolve("hotbits"))) {
                            for (Path file : files.filter(Files::isRegularFile).toList()) {
                                Files.delete(file);
                            }
                        }
                        // An intentionally oversized list exhausts even the reader's efficient in-memory bit buffer.
                        Files.writeString(directory.resolve("rates/fixture.tsv"), String.join("\n",
                                java.util.stream.IntStream.range(0, 40000).mapToObj(index -> "Rate " + index).toList()));
                        click(stage, "analysis-run");
                        phase++;
                    } else if (phase == 12 && enabled(stage, "analysis-run")) {
                        require(text(stage, "analysis-status").contains("failed"), "Missing hotbits did not report failure.");
                        require(snapshot.equals(store.load()), "Exhaustion replaced the completed result.");
                        System.out.println("Analysis UI passed: scoring, filtering, GV, case save/reopen, duplicate protection, cancellation, exhaustion, font and footer.");
                        Gdx.app.exit();
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        }, configuration);
        var restoredSettings = new SettingsRepository(new FileHandle(directory.toFile())).load();
        require(restoredSettings.getLastRateList().equals("fixture.tsv"), "Last selected rate list was not saved on close.");
    }

    private static boolean enabled(Stage stage, String id) {
        TextButton button = stage.getRoot().findActor(id);
        return button != null && !button.isDisabled();
    }

    private static void requireCompactRows(Stage stage) {
        Label first = stage.getRoot().findActor("analysis-result-0");
        Label second = stage.getRoot().findActor("analysis-result-1");
        require(first != null && second != null, "Missing compact result rows.");
        require(first.getHeight() <= first.getPrefHeight() + 1, "Result row has excessive vertical padding.");
        require(Math.abs(first.getY() - second.getY()) <= first.getPrefHeight() + 1,
                "Results have card-sized gaps between rows.");
    }

    private static void requireNextRate(Stage stage, String expected) {
        de.isuret.polos.aopi.analysis.AnalysisResultsTable results = stage.getRoot().findActor("analysis-results");
        var actor = results.getNextRate();
        require(expected == null ? actor == null : actor != null && expected.equals(actor.getName()),
                "Next-GV outline targets the wrong rate.");
    }

    private static void requireGreenOutline(Stage stage) {
        de.isuret.polos.aopi.analysis.AnalysisResultsTable results = stage.getRoot().findActor("analysis-results");
        var row = results.getNextRate();
        var position = results.localToStageCoordinates(new com.badlogic.gdx.math.Vector2(1,
                row.getY() + row.getHeight() / 2));
        float scaleX = Gdx.graphics.getBackBufferWidth() / stage.getWidth();
        float scaleY = Gdx.graphics.getBackBufferHeight() / stage.getHeight();
        var pixel = com.badlogic.gdx.graphics.Pixmap.createFromFrameBuffer(
                (int) (position.x * scaleX), (int) (position.y * scaleY), 1, 1);
        try {
            require(pixel.getPixel(0, 0) == 0xA3DF39FF, "Green outline did not render around the next rate.");
        } finally {
            pixel.dispose();
        }
    }

    private static void requireHeaderPlacement(Stage stage) {
        com.badlogic.gdx.scenes.scene2d.Actor commands = stage.getRoot().findActor("analysis-commands");
        com.badlogic.gdx.scenes.scene2d.Actor information = stage.getRoot().findActor("analysis-information");
        require(information.getTop() <= commands.getY() + 1, "Target/status must sit below the command buttons.");
        Label target = stage.getRoot().findActor("analysis-target-gv");
        Label status = stage.getRoot().findActor("analysis-status");
        require(Math.abs(target.getTop() - status.getTop()) < 1 && target.getX() < status.getX(),
                "Target GV and status must share one compact row.");
    }

    private static int visibleRates(Stage stage) {
        com.badlogic.gdx.scenes.scene2d.ui.Table results = stage.getRoot().findActor("analysis-results");
        int count = 0;
        for (var actor : results.getChildren()) {
            if (actor.getName() != null && actor.getName().startsWith("analysis-result-")) {
                count++;
            }
        }
        return count;
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
}
