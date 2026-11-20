package de.isuret.polos.aopi.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.isuret.polos.aopi.AopiApplication;
import de.isuret.polos.aopi.image.*;
import de.isuret.polos.aopi.settings.SettingsRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Exercises the embedded editor with real Scene2D pointer gestures and isolated PNG files. */
public final class SigilUiSmokeTest {
    private SigilUiSmokeTest() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("lwjgl3/build"), "sigil-ui-");
        System.setProperty("aopi.dataDirectory", directory.toAbsolutePath().toString());
        SigilCollection external = new SigilCollection("External", 80, 60);
        external.getLayers().getFirst().getImage().setRGB(10, 10, 0xff00ff00);
        Path outside = external.save(directory.resolve("external"));
        byte[] original = Files.readAllBytes(outside.resolve("IMAGE_EXTERNAL.png"));
        var imported = new java.awt.image.BufferedImage(20, 10, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var graphics = imported.createGraphics();
        graphics.setColor(java.awt.Color.RED); graphics.fillRect(0, 0, 20, 10); graphics.dispose();
        Path importFile = directory.resolve("import.png");
        javax.imageio.ImageIO.write(imported, "png", importFile.toFile());
        run(directory, outside, importFile, false);
        require(new SigilRepository(directory).loadDraft().dirty(), "Unsaved image edits not preserved on close.");
        run(directory, outside, importFile, true);
        require(java.util.Arrays.equals(original, Files.readAllBytes(outside.resolve("IMAGE_EXTERNAL.png"))),
                "Opening/saving an external collection changed its original files.");
        System.out.println("SigilMaker UI passed: pointer drawing, undo, tools, layers, rename, visibility, PNG save/open/import, Broadcast transfer, unsaved cancel, close/recovery, font and footer.");
    }

    private static void run(Path directory, Path outside, Path imported, boolean recovery) {
        var repository = new SettingsRepository(new FileHandle(directory.toFile()));
        var settings = repository.load();
        var controller = new DesktopWindowController(settings);
        var configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("AOPi embedded SigilMaker check");
        configuration.setForegroundFPS(30);
        controller.configure(configuration);
        SigilFilePicker picker = new SigilFilePicker() {
            @Override
            public void choose(boolean collection, Consumer<Path> selected, Consumer<String> error) {
                selected.accept(collection ? outside : imported);
            }
            @Override
            public void chooseColor(int currentArgb, IntConsumer selected) { selected.accept(0xffff0000); }
            @Override
            public void pasteImage(Consumer<java.awt.image.BufferedImage> selected, Consumer<String> error) {
                try { selected.accept(javax.imageio.ImageIO.read(imported.toFile())); }
                catch (Exception exception) { throw new RuntimeException(exception); }
            }
        };
        new Lwjgl3Application(new AopiApplication(settings, repository, controller, false, null, null, null, picker) {
            private int frames;
            private int phase;

            @Override
            public void render() {
                super.render();
                Stage stage = (Stage) ((InputMultiplexer) Gdx.input.getInputProcessor()).getProcessors().get(1);
                if (++frames > 500) { throw new AssertionError("Sigil UI timed out at phase " + phase); }
                try {
                    if (phase == 0 && frames > 8) {
                        click(stage, "image-tab"); phase++;
                    } else if (phase == 1 && ready(stage)) {
                        if (recovery) {
                            require(page(stage).editor().collection().getName().equals("IMAGE_RECOVERY"), "Recovery title lost.");
                            require(page(stage).editor().isDirty(), "Recovery dirty state lost.");
                            require(page(stage).editor().preview().getRGB(2, 2) == 0xffff0000, "Recovered image pixels lost.");
                            click(stage, "sigil-save"); phase = 20;
                        } else {
                            click(stage, "sigil-new"); name(stage, "Test Sigil"); phase++;
                        }
                    } else if (phase == 2 && stage.getRoot().findActor("sigil-name-dialog") == null) {
                        draw(stage, 100, 100, 200, 100); phase++;
                    } else if (phase == 3) {
                        require(page(stage).editor().preview().getRGB(150, 100) != 0, "Pointer pen stroke missing.");
                        click(stage, "sigil-undo");
                        require(page(stage).editor().preview().getRGB(150, 100) == 0, "Undo failed.");
                        click(stage, "sigil-add-layer"); name(stage, "Heart Chakra"); phase++;
                    } else if (phase == 4 && stage.getRoot().findActor("sigil-name-dialog") == null) {
                        click(stage, "sigil-color");
                        TextField hex = stage.getRoot().findActor("sigil-color-hex");
                        hex.setText("FF0000");
                        var swatch = stage.getRoot().findActor("sigil-color-preview");
                        require(swatch instanceof com.badlogic.gdx.scenes.scene2d.ui.Image && swatch.getColor().r == 1
                                && swatch.getColor().g == 0, "Solid color preview missing or incorrect: " + swatch.getColor());
                        click(stage, "sigil-color-apply"); phase = 41;
                    } else if (phase == 41 && stage.getRoot().findActor("sigil-color-dialog") == null) {
                        require(page(stage).editor().ink() == 0xffff0000, "Color chooser did not apply red.");
                        click(stage, "sigil-tool-circle");
                        draw(stage, 360, 200, 400, 200); phase = 5;
                    } else if (phase == 5) {
                        require(page(stage).editor().preview().getRGB(400, 200) != 0, "Circle tool missing pixels.");
                        click(stage, "sigil-visible-1");
                        require(page(stage).editor().preview().getRGB(400, 200) == 0, "Layer visibility failed.");
                        click(stage, "sigil-visible-1");
                        click(stage, "sigil-rename-rate"); name(stage, "Vitality");
                        click(stage, "sigil-paste"); phase = 51;
                    } else if (phase == 51 && stage.getRoot().findActor("sigil-name-dialog") == null) {
                        draw(stage, "sigil-placement-canvas", 360, 202, 100, 100);
                        draw(stage, "sigil-placement-canvas", 110, 105, 130, 105);
                        require(page(stage).editor().preview().getRGB(95, 100) == 0, "Paste preview changed layer before Apply.");
                        click(stage, "sigil-placement-cancel");
                        require(page(stage).editor().preview().getRGB(95, 100) == 0, "Cancel committed a clipboard image.");
                        click(stage, "sigil-paste"); phase = 52;
                    } else if (phase == 52) {
                        draw(stage, "sigil-placement-canvas", 360, 202, 100, 100);
                        click(stage, "sigil-placement-larger");
                        click(stage, "sigil-placement-apply");
                        require(page(stage).editor().preview().getRGB(95, 100) == 0xffff0000, "Placed clipboard image missing.");
                        click(stage, "sigil-undo");
                        require(page(stage).editor().preview().getRGB(95, 100) == 0, "Pasted image undo failed.");
                        click(stage, "sigil-save"); phase = 6;
                    } else if (phase == 6 && ready(stage)) {
                        Path folder = directory.resolve("data_images/IMAGE_TEST_SIGIL");
                        require(Files.exists(folder.resolve("Vitality.png")), "Renamed rate PNG not saved.");
                        require(SigilCollection.open(folder).getLayers().size() == 2, "Collection save format failed.");
                        click(stage, "sigil-broadcast");
                        require(((TextButton) stage.getRoot().findActor("broadcast-tab")).isChecked(), "Broadcast transfer failed.");
                        require(settings.isEnabled(de.isuret.polos.aopi.settings.Setting.BROADCAST_IMAGES), "Broadcast layers not enabled.");
                        click(stage, "image-tab");
                        click(stage, "sigil-open"); phase = 61;
                    } else if (phase == 61 && ready(stage) && stage.getRoot().findActor("sigil-open-dialog") != null) {
                        click(stage, "sigil-open-IMAGE_TEST_SIGIL"); phase = 62;
                    } else if (phase == 62 && ready(stage)) {
                        require(page(stage).editor().collection().getName().equals("IMAGE_TEST_SIGIL"), "Saved collection picker failed.");
                        click(stage, "sigil-open"); phase = 63;
                    } else if (phase == 63 && ready(stage) && stage.getRoot().findActor("sigil-open-browse") != null) {
                        click(stage, "sigil-open-browse"); phase = 7;
                    } else if (phase == 7 && ready(stage)) {
                        require(page(stage).editor().collection().getName().equals("IMAGE_EXTERNAL"), "Open collection failed.");
                        click(stage, "sigil-save"); phase++;
                    } else if (phase == 8 && ready(stage)) {
                        require(Files.exists(directory.resolve("data_images/IMAGE_EXTERNAL/IMAGE_EXTERNAL.png")), "External collection was not copied into app data.");
                        click(stage, "sigil-import"); phase++;
                    } else if (phase == 9 && ready(stage)) {
                        require(page(stage).editor().preview().getRGB(2, 2) == 0xffff0000, "Image import failed.");
                        click(stage, "sigil-new");
                        require(stage.getRoot().findActor("sigil-unsaved-dialog") != null, "Unsaved edits not protected.");
                        click(stage, "sigil-unsaved-cancel");
                        click(stage, "sigil-rename-title"); name(stage, "Recovery");
                        click(stage, "settings-tab"); settings.setFontSize(32); phase++;
                    } else if (phase == 10) {
                        click(stage, "image-tab"); phase = 11;
                        Gdx.graphics.setWindowedMode(800, 600);
                    } else if (phase == 11) {
                        NavigationLayoutSmokeTest.requireVisibleFooter(stage);
                        var title = (com.badlogic.gdx.scenes.scene2d.ui.Label) stage.getRoot().findActor("sigil-title");
                        require(title.getStyle().font.getCapHeight() > 16, "IMAGE text did not follow font setting.");
                        Gdx.app.exit();
                    } else if (phase == 20 && ready(stage)) {
                        require(Files.exists(directory.resolve("data_images/IMAGE_RECOVERY/IMAGE_RECOVERY.png")), "Recovered collection did not save.");
                        Gdx.app.exit();
                    }
                } catch (Exception exception) { throw new RuntimeException(exception); }
            }
        }, configuration);
    }

    private static SigilPage page(Stage stage) { return stage.getRoot().findActor("sigil-page"); }
    private static boolean ready(Stage stage) {
        TextButton button = stage.getRoot().findActor("sigil-save");
        return button != null && !button.isDisabled();
    }
    private static void name(Stage stage, String value) {
        TextField field = stage.getRoot().findActor("sigil-name-input");
        field.setText(value); click(stage, "sigil-name-ok");
    }
    private static void click(Stage stage, String id) {
        TextButton button = stage.getRoot().findActor(id);
        require(button != null && !button.isDisabled(), "Button unavailable: " + id);
        button.fire(new ChangeListener.ChangeEvent());
    }
    private static void draw(Stage stage, int x1, int y1, int x2, int y2) {
        draw(stage, "sigil-canvas", x1, y1, x2, y2);
    }

    private static void draw(Stage stage, String id, int x1, int y1, int x2, int y2) {
        SigilCanvas canvas = stage.getRoot().findActor(id);
        var bounds = canvas.imageBounds();
        var base = page(stage).editor().collection().getLayers().getFirst().getImage();
        Vector2 start = canvas.localToStageCoordinates(new Vector2(bounds.x + x1 * bounds.width / base.getWidth(),
                bounds.y + bounds.height - y1 * bounds.height / base.getHeight()));
        Vector2 end = canvas.localToStageCoordinates(new Vector2(bounds.x + x2 * bounds.width / base.getWidth(),
                bounds.y + bounds.height - y2 * bounds.height / base.getHeight()));
        stage.stageToScreenCoordinates(start); stage.stageToScreenCoordinates(end);
        require(stage.touchDown(Math.round(start.x), Math.round(start.y), 0, Input.Buttons.LEFT), "Canvas rejected pointer.");
        stage.touchDragged(Math.round(end.x), Math.round(end.y), 0);
        stage.touchUp(Math.round(end.x), Math.round(end.y), 0, Input.Buttons.LEFT);
    }
    private static void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
