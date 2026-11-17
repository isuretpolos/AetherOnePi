package de.isuret.polos.aopi.session;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Inline case editor and append-only session history. File work runs away from the render thread. */
public class SessionPage extends Table {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final CaseRepository repository;
    private final EssentialQuestions questions;
    private final Skin skin;
    private final Stage stage;
    private final Consumer<CaseData> onCaseChanged;
    private final Runnable onHotbitsConsumed;
    private final ExecutorService io = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "aopi-session-files");
        thread.setDaemon(true);
        return thread;
    });
    private final List<TextButton> buttons = new ArrayList<>();
    private final TextField name;
    private final TextArea description;
    private final TextField intention;
    private final TextArea notes;
    private final Label status;
    private final Label dates;
    private final Table history = new Table();
    private CaseData current = CaseData.create();
    private boolean recovered;
    private boolean busy;
    private boolean preserveUnreadableDraft;
    private volatile boolean disposed;
    private volatile Signature lastSavedSignature;

    private record Signature(String id, String name, String description, String intention, String notes) {
    }

    private record Saved(CaseData data, String warning) {
    }

    private record Recovery(CaseData.Draft draft, String error) {
    }

    public SessionPage(CaseRepository repository, CachedHotbits hotbits, Skin skin, Stage stage,
                       Consumer<CaseData> onCaseChanged, Runnable onHotbitsConsumed) {
        this.repository = repository;
        this.questions = new EssentialQuestions(hotbits::nextInt);
        this.skin = skin;
        this.stage = stage;
        this.onCaseChanged = onCaseChanged;
        this.onHotbitsConsumed = onHotbitsConsumed;
        top().left();
        defaults().growX().spaceBottom(12);
        add(new Label("SESSION", skin, "accent")).row();
        HorizontalGroup actions = new HorizontalGroup().wrap().left().rowLeft().space(8).wrapSpace(8);
        actions.addActor(button("NEW", "session-new", () -> replace(this::newCase)));
        actions.addActor(button("LOAD", "session-load", this::chooseCase));
        actions.addActor(button("SAVE", "session-save", () -> saveCase(null)));
        actions.addActor(button("ESSENTIAL QUESTIONS", "session-questions", this::askQuestions));
        add(actions).row();
        status = paragraph("New case. Enter a name and description, then save.");
        status.setName("session-status");
        add(status).row();
        dates = new Label("", skin, "muted");
        dates.setWrap(true);
        add(dates).row();
        add(new Label("CASE NAME", skin, "accent")).row();
        name = new TextField("", skin);
        name.setName("session-name");
        name.setMessageText("Name of the person, target, or case");
        name.setMaxLength(200);
        add(name).minHeight(42).row();
        add(new Label("CASE DESCRIPTION", skin, "accent")).row();
        description = area("session-description", 4);
        add(description).minHeight(120).row();
        add(new Label("NEW SESSION NOTE", skin, "accent")).padTop(16).row();
        add(paragraph("Add an intention and/or notes. SAVE includes this draft once; repeated saves do not add empty sessions."))
                .row();
        intention = new TextField("", skin);
        intention.setName("session-intention");
        intention.setMessageText("Intention or session title");
        intention.setMaxLength(300);
        add(intention).minHeight(42).row();
        notes = area("session-notes", 6);
        add(notes).minHeight(180).row();
        add(button("ADD SESSION + SAVE", "session-add-note", () -> {
            if (intention.getText().isBlank() && notes.getText().isBlank()) {
                status.setText("Enter an intention or note before adding a session.");
            } else {
                saveCase(null);
            }
        })).row();
        add(paragraph("Analysis and broadcast attachments become available with those features."))
                .padBottom(16).row();
        add(new Label("SESSION HISTORY", skin, "accent")).row();
        history.top().left();
        add(history).row();
        for (TextField field : new TextField[]{name, description, intention, notes}) {
            field.setProgrammaticChangeEvents(false);
            field.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    status.setText("Unsaved changes. SAVE keeps the case and session note; closing preserves a recovery draft.");
                }
            });
        }
        populate(current);
        runJob(() -> {
            try {
                return new Recovery(repository.loadDraft(), null);
            } catch (Exception exception) {
                return new Recovery(null, exception.getMessage() == null
                        ? exception.getClass().getSimpleName() : exception.getMessage());
            }
        }, recovery -> {
            if (recovery.error() != null) {
                preserveUnreadableDraft = true;
                status.setText("Could not read session-draft.json. Its contents are preserved until you save or start another case.");
            } else if (recovery.draft() != null) {
                populate(recovery.draft().caseData());
                intention.setText(recovery.draft().intention());
                notes.setText(recovery.draft().notes());
                recovered = true;
                status.setText("Recovered unsaved work from the last close. Review it and SAVE when ready.");
                onCaseChanged.accept(current);
            }
        });
    }

    private TextArea area(String id, int rows) {
        TextArea area = new TextArea("", skin);
        area.setName(id);
        area.setPrefRows(rows);
        area.setMaxLength(100000);
        area.setFocusTraversal(false);
        return area;
    }

    public boolean isDirty() {
        return recovered || !name.getText().equals(current.name()) || !description.getText().equals(current.description())
                || !intention.getText().isBlank() || !notes.getText().isBlank();
    }

    private Signature signature() {
        return new Signature(current.id(), name.getText(), description.getText(), intention.getText(), notes.getText());
    }

    private CaseData editedCase() {
        return new CaseData(current.id(), name.getText(), description.getText(), current.created(),
                current.lastChange(), current.sessionList());
    }

    private void saveCase(Runnable afterSave) {
        if (name.getText().isBlank()) {
            status.setText("Enter a case name before saving.");
            stage.setKeyboardFocus(name);
            return;
        }
        Signature signature = signature();
        List<CaseData.Entry> entries = new ArrayList<>(current.sessionList());
        if (!intention.getText().isBlank() || !notes.getText().isBlank()) {
            entries.add(new CaseData.Entry(UUID.randomUUID().toString(), Instant.now(),
                    intention.getText().trim(), notes.getText()));
        }
        CaseData toSave = new CaseData(current.id(), name.getText().trim(), description.getText(),
                current.created(), Instant.now(), entries);
        runJob(() -> {
            repository.save(toSave);
            lastSavedSignature = signature;
            String warning = "";
            try {
                repository.clearDraft();
                repository.remember(toSave.id());
            } catch (Exception exception) {
                warning = " Case saved, but recent-case or recovery metadata could not be updated.";
            }
            return new Saved(toSave, warning);
        }, saved -> {
            populate(saved.data());
            status.setText("Case saved." + saved.warning());
            onCaseChanged.accept(current);
            if (afterSave != null) {
                afterSave.run();
            }
        });
    }

    private void newCase() {
        runJob(() -> {
            repository.clearDraft();
            return CaseData.create();
        }, data -> {
            populate(data);
            status.setText("New case. Enter a name and description, then save.");
            onCaseChanged.accept(current);
        });
    }

    public void openCase(String id, Runnable afterOpen) {
        if (busy) {
            status.setText("Please wait for the current case operation to finish.");
            return;
        }
        replace(() -> runJob(() -> {
            CaseData data = repository.load(id);
            repository.clearDraft();
            String warning = "";
            try {
                repository.remember(id);
            } catch (Exception exception) {
                warning = " Recent cases could not be updated.";
            }
            return new Saved(data, warning);
        }, loaded -> {
            populate(loaded.data());
            status.setText("Case loaded." + loaded.warning());
            onCaseChanged.accept(current);
            if (afterOpen != null) {
                afterOpen.run();
            }
        }));
    }

    private void chooseCase() {
        runJob(repository::list, listing -> {
            Dialog dialog = new Dialog("Load case", skin);
            dialog.setName("session-load-dialog");
            Table list = new Table();
            list.top().left();
            if (listing.cases().isEmpty()) {
                list.add(paragraph("No saved cases in this application's case folder.")).growX().row();
            }
            for (CaseData data : listing.cases()) {
                TextButton choice = new TextButton(data.name() + "\n" + DATE.format(data.lastChange()), skin);
                choice.setName("session-load-" + data.id());
                choice.getLabel().setWrap(true);
                choice.pad(12);
                choice.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        dialog.hide();
                        openCase(data.id(), null);
                    }
                });
                list.add(choice).growX().padBottom(8).row();
            }
            if (!listing.unreadableFiles().isEmpty()) {
                list.add(paragraph(listing.unreadableFiles().size() + " unreadable or unsupported case files were skipped."))
                        .growX().row();
            }
            ScrollPane scroll = new ScrollPane(list, skin);
            scroll.setScrollingDisabled(true, false);
            dialog.getContentTable().add(scroll).width(Math.min(620, stage.getWidth() - 100))
                    .height(stage.getHeight() * 0.5f).pad(16);
            dialog.button("CANCEL");
            dialog.key(Input.Keys.ESCAPE, null);
            dialog.show(stage);
        });
    }

    private void replace(Runnable action) {
        if (!isDirty()) {
            action.run();
            return;
        }
        Dialog dialog = new Dialog("Unsaved Session changes", skin) {
            @Override
            protected void result(Object result) {
                if ("save".equals(result)) {
                    saveCase(action);
                } else if ("discard".equals(result)) {
                    action.run();
                }
            }
        };
        dialog.setName("session-unsaved-dialog");
        dialog.getContentTable().add(paragraph("Save the case and session draft before continuing?"))
                .width(Math.min(560, stage.getWidth() - 100)).pad(20);
        TextButton save = new TextButton("SAVE + CONTINUE", skin);
        save.setName("session-unsaved-save");
        TextButton discard = new TextButton("DISCARD", skin);
        discard.setName("session-unsaved-discard");
        TextButton cancel = new TextButton("CANCEL", skin);
        cancel.setName("session-unsaved-cancel");
        dialog.button(save, "save");
        dialog.button(discard, "discard");
        dialog.button(cancel, "cancel");
        dialog.key(Input.Keys.ESCAPE, "cancel");
        dialog.show(stage);
    }

    private void askQuestions() {
        runJob(questions::ask, result -> {
            if (intention.getText().isBlank()) {
                intention.setText("Essential questions");
            }
            notes.setText(notes.getText() + (notes.getText().isBlank() ? "" : "\n\n") + result);
            status.setText("Questions added to the session draft. Review and SAVE to keep them.");
            onHotbitsConsumed.run();
        });
    }

    private void populate(CaseData data) {
        current = data;
        recovered = false;
        name.setText(data.name());
        description.setText(data.description());
        intention.setText("");
        notes.setText("");
        dates.setText("Created: " + DATE.format(data.created()) + " | Last change: " + DATE.format(data.lastChange())
                + " | Sessions: " + data.sessionList().size());
        history.clearChildren();
        if (data.sessionList().isEmpty()) {
            history.add(paragraph("No session notes yet.")).growX();
        }
        for (int index = data.sessionList().size() - 1; index >= 0; index--) {
            CaseData.Entry entry = data.sessionList().get(index);
            Table card = new Table();
            card.pad(14);
            card.setBackground(skin.newDrawable("white", Color.valueOf(index % 2 == 0 ? "292238" : "211C2D")));
            card.add(paragraph(DATE.format(entry.created()) + " | " + entry.intention())).growX().row();
            card.add(paragraph(entry.description())).growX().padTop(8);
            history.add(card).growX().padBottom(8).row();
        }
    }

    private <T> void runJob(Callable<T> task, Consumer<T> success) {
        if (busy || disposed) {
            return;
        }
        setBusy(true);
        io.execute(() -> {
            try {
                T result = task.call();
                Gdx.app.postRunnable(() -> {
                    if (!disposed) {
                        setBusy(false);
                        success.accept(result);
                    }
                });
            } catch (Exception exception) {
                Gdx.app.postRunnable(() -> {
                    if (!disposed) {
                        setBusy(false);
                        status.setText("Session operation failed: " + exception.getMessage());
                    }
                });
            }
        });
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        buttons.forEach(button -> button.setDisabled(busy));
        name.setDisabled(busy);
        description.setDisabled(busy);
        intention.setDisabled(busy);
        notes.setDisabled(busy);
    }

    public void dispose() {
        CaseData.Draft draft = new CaseData.Draft(editedCase(), intention.getText(), notes.getText());
        Signature signature = signature();
        boolean dirty = isDirty();
        disposed = true;
        io.execute(() -> {
            try {
                if (dirty && !signature.equals(lastSavedSignature)) {
                    repository.saveDraft(draft);
                } else if (!preserveUnreadableDraft || signature.equals(lastSavedSignature)) {
                    repository.clearDraft();
                }
            } catch (Exception exception) {
                Gdx.app.error("Session", "Cannot preserve Session draft on close.", exception);
            }
        });
        io.shutdown();
        try {
            io.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private TextButton button(String text, String id, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.setName(id);
        button.pad(10, 12, 10, 12);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        buttons.add(button);
        return button;
    }

    private Label paragraph(String text) {
        Label label = new Label(text, skin);
        label.setWrap(true);
        return label;
    }
}
