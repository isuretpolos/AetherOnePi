package de.isuret.polos.aopi.image;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Embedded SigilMaker UI. File operations use snapshots on one IO worker; drawing stays on the render thread. */
public class SigilPage extends Table {
    private final SigilMaker editor = new SigilMaker();
    private final SigilRepository repository;
    private final SigilFilePicker picker;
    private final Skin skin;
    private final Consumer<Path> broadcast;
    private final SigilCanvas canvas;
    private final Table layerList = new Table();
    private final Label title;
    private final Label status;
    private final Label brush;
    private final TextButton undo;
    private final List<TextButton> controls = new ArrayList<>();
    private final java.util.Map<SigilMaker.Tool, TextButton> tools = new java.util.EnumMap<>(SigilMaker.Tool.class);
    private final ExecutorService io = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "aopi-sigil-files");
        thread.setDaemon(true);
        return thread;
    });
    private boolean busy;
    private boolean disposed;
    private long shownRevision = -1;
    private Path savedDirectory;
    private boolean used;
    private volatile boolean preserveUnreadableDraft;
    private SigilCanvas placementCanvas;

    public SigilPage(Skin skin, SigilRepository repository, SigilFilePicker picker, Consumer<Path> broadcast) {
        this.skin = skin;
        this.repository = repository;
        this.picker = picker;
        this.broadcast = broadcast;
        setName("sigil-page");
        top().left();
        defaults().growX().spaceBottom(6);
        title = new Label("", skin, "accent");
        title.setName("sigil-title");
        status = paragraph("Draw the base, then add named transparent rate layers.");
        status.setName("sigil-status");
        brush = new Label("4 px", skin);
        HorizontalGroup actions = new HorizontalGroup().wrap().left().space(6).wrapSpace(6);
        actions.addActor(button("NEW", "sigil-new", () -> guard(() -> prompt("New collection", "UNTITLED", value -> {
            editor.reset(new SigilCollection(value, 720, 405), true);
            savedDirectory = null;
        }))));
        TextButton open = button("OPEN", "sigil-open", () -> guard(this::showCollections));
        actions.addActor(open);
        actions.addActor(button("SAVE", "sigil-save", () -> save(null)));
        actions.addActor(button("ADD LAYER", "sigil-add-layer", () -> prompt("Rate / layer name", "", editor::addLayer)));
        TextButton importImage = button("IMPORT IMAGE", "sigil-import", () -> picker.choose(false, path -> runJob(() -> {
            var image = ImageIO.read(path.toFile());
            if (image == null) { throw new IllegalArgumentException("Choose a supported image file."); }
            return image;
        }, image -> { editor.importImage(image); status.setText("Image imported into the selected layer."); }), status::setText));
        actions.addActor(importImage);
        actions.addActor(button("PASTE IMAGE", "sigil-paste", this::pasteImage));
        TextButton color = button("COLOR", "sigil-color", this::showColorChooser);
        actions.addActor(color);
        undo = button("UNDO", "sigil-undo", editor::undo);
        actions.addActor(undo);
        actions.addActor(button("CLEAR", "sigil-clear", editor::clear));
        actions.addActor(button("SEND TO BROADCAST", "sigil-broadcast", () -> save(broadcast)));
        add(actions).row();
        if (picker == null) {
            importImage.setDisabled(true);
            controls.stream().filter(button -> "sigil-paste".equals(button.getName()))
                    .forEach(button -> button.setDisabled(true));
        }

        HorizontalGroup drawing = new HorizontalGroup().wrap().left().space(6).wrapSpace(6);
        for (var tool : SigilMaker.Tool.values()) {
            TextButton button = button(tool.name(), "sigil-tool-" + tool.name().toLowerCase(java.util.Locale.ROOT), () -> editor.setTool(tool));
            tools.put(tool, button);
            drawing.addActor(button);
        }
        drawing.addActor(button("−", "sigil-brush-less", () -> editor.setBrushSize(editor.brushSize() - 1)));
        drawing.addActor(brush);
        drawing.addActor(button("+", "sigil-brush-more", () -> editor.setBrushSize(editor.brushSize() + 1)));
        drawing.addActor(button("RENAME TITLE", "sigil-rename-title", () -> prompt("Collection title", editor.collection().getName(), editor::renameTitle)));
        drawing.addActor(button("RENAME RATE", "sigil-rename-rate", () -> prompt("Rate / layer name",
                editor.collection().getLayers().get(editor.selectedLayer()).getName(), editor::renameRate)));
        add(drawing).row();
        add(title).row();
        add(status).row();
        Table body = new Table();
        layerList.top().left();
        ScrollPane layers = new ScrollPane(layerList, skin);
        layers.setName("sigil-layers");
        layers.setScrollingDisabled(true, false);
        layers.setFadeScrollBars(false);
        layers.setFlickScroll(false);
        body.add(layers).width(new Value() {
            @Override
            public float get(Actor context) { return Math.min(280, Math.max(180, skin.getFont("default-font").getCapHeight() * 12)); }
        }).growY().minHeight(240).padRight(12);
        canvas = new SigilCanvas(editor, skin);
        body.add(canvas).grow().minWidth(0).minHeight(240);
        add(body).grow().minHeight(240).row();
        runJob(() -> {
            try { return repository.loadDraft(); }
            catch (Exception exception) { preserveUnreadableDraft = true; throw exception; }
        }, draft -> {
            if (draft != null) {
                used = true;
                editor.reset(draft.collection(), draft.dirty());
                editor.selectLayer(Math.min(Math.max(0, draft.selected()), draft.collection().getLayers().size() - 1));
                editor.setTool(draft.tool()); editor.setBrushSize(draft.brush()); editor.setInk(draft.ink());
                status.setText(draft.dirty() ? "Recovered unsaved sigil edits." : "Restored the last sigil collection.");
            }
        });
    }

    public SigilMaker editor() { return editor; }
    public void onShown() { used = true; }

    private void pasteImage() {
        if (!editor.collection().getLayers().get(editor.selectedLayer()).isVisible()) {
            status.setText("Select a visible layer before pasting an image.");
            return;
        }
        picker.pasteImage(image -> {
            if (!disposed) { showPlacement(image); }
        }, message -> {
            if (!disposed) { status.setText(message); }
        });
    }

    private void showPlacement(java.awt.image.BufferedImage image) {
        var base = editor.collection().getLayers().getFirst().getImage();
        SigilPlacement placement = new SigilPlacement(image, base.getWidth(), base.getHeight());
        SigilCanvas preview = new SigilCanvas(editor, skin, placement);
        preview.setName("sigil-placement-canvas");
        placementCanvas = preview;
        Dialog dialog = new Dialog("Place clipboard image", skin) {
            @Override
            protected void result(Object result) {
                if (Boolean.TRUE.equals(result)) {
                    editor.applyPlacement(placement);
                    status.setText("Clipboard image pasted. UNDO removes this placement.");
                }
                preview.dispose();
                placementCanvas = null;
                // Remove immediately so a disposed preview cannot be drawn during the fade.
                hide(null);
            }
        };
        dialog.setName("sigil-placement-dialog");
        Table content = dialog.getContentTable();
        content.defaults().pad(6);
        content.add(paragraph("Drag the image to move it. Drag its lower-right green handle to resize. Aspect ratio is preserved."))
                .growX().row();
        content.add(preview).width(new Value() {
            @Override
            public float get(Actor context) { return Math.min(1000, getStage().getWidth() - 80); }
        }).height(new Value() {
            @Override
            public float get(Actor context) { return Math.max(120, Math.min(480, getStage().getHeight() - 260)); }
        }).row();
        HorizontalGroup sizing = new HorizontalGroup().space(6);
        sizing.addActor(dialogButton("SMALLER", "sigil-placement-smaller",
                () -> placement.resize(Math.min(placement.width() - 1, Math.round(placement.width() * 0.9f)))));
        sizing.addActor(dialogButton("LARGER", "sigil-placement-larger",
                () -> placement.resize(Math.max(placement.width() + 1, Math.round(placement.width() * 1.1f)))));
        content.add(sizing).left().row();
        TextButton apply = new TextButton("APPLY", skin);
        apply.setName("sigil-placement-apply");
        TextButton cancel = new TextButton("CANCEL", skin);
        cancel.setName("sigil-placement-cancel");
        dialog.button(apply, true);
        dialog.button(cancel, false);
        dialog.key(Input.Keys.ESCAPE, false);
        dialog.show(getStage());
    }

    /** Keep saved collections accessible without a second desktop window. */
    private void showCollections() {
        runJob(() -> {
            if (!Files.isDirectory(repository.collections())) {
                return List.<Path>of();
            }
            try (var entries = Files.list(repository.collections())) {
                return entries.filter(Files::isDirectory)
                        .filter(path -> Files.isRegularFile(path.resolve(path.getFileName() + ".png")))
                        .sorted(java.util.Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
            }
        }, folders -> {
            Dialog dialog = new Dialog("Open sigil collection", skin);
            dialog.setName("sigil-open-dialog");
            Table entries = new Table();
            entries.top().defaults().growX().padBottom(6);
            for (Path folder : folders) {
                entries.add(dialogButton(folder.getFileName().toString(), "sigil-open-" + folder.getFileName(), () -> {
                    dialog.hide();
                    open(folder);
                })).row();
            }
            if (folders.isEmpty()) {
                entries.add(paragraph("No saved collections yet. Save a collection or browse for an existing IMAGE_<NAME> folder.")).row();
            }
            ScrollPane scroll = new ScrollPane(entries, skin);
            scroll.setScrollingDisabled(true, false);
            scroll.setFadeScrollBars(false);
            scroll.setFlickScroll(false);
            dialog.getContentTable().add(scroll).width(Math.min(580, getStage().getWidth() - 60))
                    .height(Math.min(360, getStage().getHeight() - 180)).pad(12);
            if (picker != null) {
                dialog.getButtonTable().add(dialogButton("BROWSE…", "sigil-open-browse", () -> {
                    dialog.hide();
                    picker.choose(true, this::open, status::setText);
                }));
            }
            dialog.button("CANCEL");
            dialog.key(Input.Keys.ESCAPE, null);
            dialog.show(getStage());
            getStage().setScrollFocus(scroll);
        });
    }

    /** Scene2D controls also work in fullscreen and follow the application's font setting. */
    private void showColorChooser() {
        Dialog dialog = new Dialog("Sigil drawing color", skin);
        dialog.setName("sigil-color-dialog");
        Table content = dialog.getContentTable();
        content.defaults().pad(6);
        TextField hex = new TextField(String.format("%06X", editor.ink() & 0xffffff), skin);
        hex.setProgrammaticChangeEvents(true);
        hex.setName("sigil-color-hex");
        hex.setMaxLength(7);
        Image preview = new Image(skin.getDrawable("white"));
        preview.setName("sigil-color-preview");
        Label error = paragraph("");
        Slider[] channels = new Slider[3];
        Label[] values = new Label[3];
        boolean[] updating = {false};
        Runnable refresh = () -> {
            int rgb = ((int) channels[0].getValue() << 16) | ((int) channels[1].getValue() << 8)
                    | (int) channels[2].getValue();
            preview.setColor(new com.badlogic.gdx.graphics.Color((rgb << 8) | 0xff));
            for (int index = 0; index < channels.length; index++) {
                values[index].setText(Integer.toString((int) channels[index].getValue()));
            }
        };
        String[] names = {"Red", "Green", "Blue"};
        for (int index = 0; index < channels.length; index++) {
            Slider slider = new Slider(0, 255, 1, false, skin);
            slider.setName("sigil-color-" + names[index].toLowerCase(java.util.Locale.ROOT));
            slider.setValue((editor.ink() >>> (16 - index * 8)) & 0xff);
            channels[index] = slider;
            values[index] = new Label("", skin);
            content.add(new Label(names[index], skin)).left();
            content.add(slider).width(Math.min(260, getStage().getWidth() - 220));
            content.add(values[index]).width(50).row();
            slider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (updating[0]) { return; }
                    updating[0] = true;
                    hex.setText(String.format("%02X%02X%02X", (int) channels[0].getValue(),
                            (int) channels[1].getValue(), (int) channels[2].getValue()));
                    refresh.run();
                    error.setText("");
                    updating[0] = false;
                }
            });
        }
        content.add(new Label("Hex", skin)).left();
        content.add(hex).growX();
        content.add(preview).width(64).height(32).row();
        content.add(error).colspan(3).growX().row();
        hex.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (updating[0]) { return; }
                String text = hex.getText().replaceFirst("^#", "");
                if (!text.matches("[0-9a-fA-F]{6}")) {
                    error.setText("Enter six hexadecimal digits, for example FF8800.");
                    return;
                }
                updating[0] = true;
                int rgb = Integer.parseInt(text, 16);
                for (int index = 0; index < channels.length; index++) {
                    channels[index].setValue((rgb >>> (16 - index * 8)) & 0xff);
                }
                refresh.run();
                error.setText("");
                updating[0] = false;
            }
        });
        dialog.getButtonTable().add(dialogButton("APPLY", "sigil-color-apply", () -> {
            String text = hex.getText().replaceFirst("^#", "");
            if (!text.matches("[0-9a-fA-F]{6}")) {
                error.setText("Enter six hexadecimal digits, for example FF8800.");
                return;
            }
            editor.setInk(0xff000000 | Integer.parseInt(text, 16));
            status.setText("Drawing color: #" + text.toUpperCase(java.util.Locale.ROOT));
            dialog.hide();
        }));
        dialog.button("CANCEL");
        dialog.key(Input.Keys.ESCAPE, null);
        refresh.run();
        dialog.show(getStage());
        getStage().setKeyboardFocus(hex);
    }

    private void open(Path folder) {
        runJob(() -> SigilCollection.open(folder), collection -> {
            editor.reset(collection, false);
            savedDirectory = null;
            status.setText("Opened " + collection.getName() + ". SAVE writes a copy into this app's data_images folder.");
        });
    }

    private void save(Consumer<Path> after) {
        if (savedDirectory != null && !editor.isDirty()) {
            if (after != null) { after.accept(savedDirectory); }
            return;
        }
        Runnable write = () -> {
            SigilCollection snapshot = editor.collection().copy();
            runJob(() -> repository.save(snapshot), path -> {
                editor.markSaved(snapshot);
                savedDirectory = path;
                status.setText("Saved to " + path);
                if (after != null) { after.accept(path); }
            });
        };
        if (Files.exists(repository.collections().resolve(editor.collection().getName()))) {
            confirm("Replace images in " + editor.collection().getName() + "?", write);
        } else { write.run(); }
    }

    private void guard(Runnable next) {
        if (!editor.isDirty()) { next.run(); return; }
        Dialog dialog = new Dialog("Unsaved sigil changes", skin);
        dialog.setName("sigil-unsaved-dialog");
        dialog.getContentTable().add(paragraph("Save your current collection before continuing?")).width(420).pad(16);
        dialog.getButtonTable().add(dialogButton("SAVE", "sigil-unsaved-save", () -> { dialog.hide(); save(path -> next.run()); }));
        dialog.getButtonTable().add(dialogButton("DISCARD", "sigil-unsaved-discard", () -> { dialog.hide(); next.run(); }));
        dialog.getButtonTable().add(dialogButton("CANCEL", "sigil-unsaved-cancel", dialog::hide));
        dialog.key(Input.Keys.ESCAPE, null);
        dialog.show(getStage());
    }

    private void confirm(String message, Runnable action) {
        Dialog dialog = new Dialog("Save collection", skin);
        dialog.setName("sigil-replace-dialog");
        dialog.getContentTable().add(paragraph(message)).width(420).pad(16);
        dialog.getButtonTable().add(dialogButton("SAVE", "sigil-replace-save", () -> { dialog.hide(); action.run(); }));
        dialog.button("CANCEL");
        dialog.key(Input.Keys.ESCAPE, null);
        dialog.show(getStage());
    }

    private void prompt(String heading, String initial, Consumer<String> action) {
        Dialog dialog = new Dialog(heading, skin);
        dialog.setName("sigil-name-dialog");
        TextField value = new TextField(initial, skin);
        value.setName("sigil-name-input");
        value.setMaxLength(100);
        Label error = paragraph("");
        dialog.getContentTable().add(value).width(Math.min(480, getStage().getWidth() - 100)).pad(12).row();
        dialog.getContentTable().add(error).growX().pad(12);
        dialog.getButtonTable().add(dialogButton("OK", "sigil-name-ok", () -> {
            try { action.accept(value.getText()); dialog.hide(); update(); }
            catch (IllegalArgumentException exception) { error.setText(exception.getMessage()); }
        }));
        dialog.button("CANCEL");
        dialog.key(Input.Keys.ESCAPE, null);
        dialog.show(getStage());
        getStage().setKeyboardFocus(value);
    }

    public void update() {
        brush.setText(editor.brushSize() + " px");
        for (var entry : tools.entrySet()) { entry.getValue().setChecked(entry.getKey() == editor.tool()); }
        undo.setDisabled(busy || !editor.canUndo());
        title.setText((editor.isDirty() ? "* " : "") + editor.collection().getName());
        if (shownRevision == editor.revision()) { return; }
        shownRevision = editor.revision();
        layerList.clearChildren();
        for (int index = 0; index < editor.collection().getLayers().size(); index++) {
            int position = index;
            var layer = editor.collection().getLayers().get(index);
            TextButton select = dialogButton(layer.getName(), "sigil-layer-" + index, () -> editor.selectLayer(position));
            select.setChecked(index == editor.selectedLayer());
            select.setDisabled(busy);
            select.getLabel().setEllipsis(true);
            layerList.add(select).growX().minWidth(0).padBottom(4);
            TextButton visible = dialogButton(layer.isVisible() ? "ON" : "OFF", "sigil-visible-" + index,
                    () -> editor.toggleVisible(position));
            visible.setDisabled(busy);
            layerList.add(visible).padLeft(4).padBottom(4).row();
        }
    }

    private <T> void runJob(Callable<T> task, Consumer<T> done) {
        if (busy || disposed) { return; }
        setBusy(true);
        io.execute(() -> {
            try {
                T result = task.call();
                Gdx.app.postRunnable(() -> { if (!disposed) { setBusy(false); done.accept(result); update(); } });
            } catch (Exception exception) {
                Gdx.app.postRunnable(() -> { if (!disposed) { setBusy(false); status.setText("Sigil operation failed: " + exception.getMessage()); } });
            }
        });
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        controls.forEach(button -> button.setDisabled(busy));
        if (picker == null) {
            controls.stream().filter(button -> List.of("sigil-import", "sigil-paste").contains(button.getName()))
                    .forEach(button -> button.setDisabled(true));
        }
        canvas.setEnabled(!busy);
        shownRevision = -1;
        update();
    }

    public void dispose() {
        editor.endGesture();
        var draft = new SigilRepository.Draft(editor.collection().copy(), editor.isDirty(), editor.selectedLayer(),
                editor.tool(), editor.brushSize(), editor.ink());
        disposed = true;
        if ((used || editor.isDirty()) && (!preserveUnreadableDraft || editor.isDirty())) {
            io.execute(() -> {
                try { repository.saveDraft(draft); }
                catch (Exception exception) { Gdx.app.error("SigilMaker", "Cannot preserve sigil recovery snapshot.", exception); }
            });
        }
        io.shutdown();
        try { io.awaitTermination(10, TimeUnit.SECONDS); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
        canvas.dispose();
        if (placementCanvas != null) { placementCanvas.dispose(); }
    }

    private TextButton button(String text, String id, Runnable action) {
        TextButton button = dialogButton(text, id, action);
        controls.add(button);
        return button;
    }

    private TextButton dialogButton(String text, String id, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.setName(id);
        button.setProgrammaticChangeEvents(false);
        button.pad(7, 10, 7, 10);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) { action.run(); }
        });
        return button;
    }

    private Label paragraph(String text) {
        Label label = new Label(text, skin);
        label.setWrap(true);
        return label;
    }
}
