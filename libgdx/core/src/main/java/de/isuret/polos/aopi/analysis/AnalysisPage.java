package de.isuret.polos.aopi.analysis;

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
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import de.isuret.polos.aopi.session.CachedHotbits;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.Setting;

import java.io.InterruptedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

/** UI owns immutable completed results; the worker owns list IO and all random draws. */
public class AnalysisPage extends Table {

    private final Skin skin;
    private final Stage stage;
    private final RateRepository rates;
    private final AnalysisStore store;
    private final ApplicationSettings settings;
    private final RateFilePicker filePicker;
    private final Consumer<AnalysisResult> saveToCase;
    private final Runnable consumed;
    private final Supplier<String> caseId;
    private final AnalysisEngine engine;
    private final List<TextButton> controls = new ArrayList<>();
    private final Label status;
    private final Label selectedLabel;
    private final Label targetVitality;
    private final Label summary;
    private final TextField filter;
    private final Table results = new Table();
    private final Table header = new Table();
    private final Table commands = new Table();
    private final Table information = new Table();
    private boolean wideHeader;
    private final TextButton stop;
    private final TextButton analyze;
    private final TextButton gvNext;
    private final TextButton gvAll;
    private final TextButton copy;
    private final TextButton save;
    private final TextButton importLists;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "aopi-analysis");
        thread.setDaemon(true);
        return thread;
    });
    private volatile Thread activeThread;
    private volatile boolean cancelled;
    private volatile boolean disposed;
    private volatile String progress = "";
    private volatile String calculationProgress = "";
    private volatile long drawsUsed;
    private final Object completionLock = new Object();
    private boolean committed;
    private boolean busy;
    private String selected;
    private String resultCaseId;
    private AnalysisResult result;

    public AnalysisPage(Skin skin, Stage stage, RateRepository rates, AnalysisStore store,
                        CachedHotbits hotbits, ApplicationSettings settings, RateFilePicker filePicker,
                        Consumer<AnalysisResult> saveToCase, Runnable consumed, Supplier<String> caseId,
                        BooleanSupplier acquisitionActive) {
        this.skin = skin;
        this.stage = stage;
        this.rates = rates;
        this.store = store;
        this.settings = settings;
        this.filePicker = filePicker;
        this.saveToCase = saveToCase;
        this.consumed = consumed;
        this.caseId = caseId;
        engine = new AnalysisEngine(bound -> {
            if (cancelled) {
                throw new InterruptedIOException("Analysis cancelled.");
            }
            int value = hotbits.nextInt(bound, acquisitionActive, () -> progress = calculationProgress
                    + " | Waiting for fresh hotbits from acquisition... | Draws used: " + drawsUsed);
            drawsUsed++;
            progress = calculationProgress + " | Draws used: " + drawsUsed;
            return value;
        }, message -> {
            calculationProgress = message;
            progress = message + " | Draws used: " + drawsUsed;
        });
        top().left();
        defaults().growX().spaceBottom(12);
        commands.top().left();
        commands.setName("analysis-commands");
        commands.defaults().growX().spaceBottom(10);
        information.top().left();
        information.setName("analysis-information");
        information.defaults().growX().spaceBottom(6);
        Table heading = new Table();
        heading.setName("analysis-heading");
        targetVitality = new Label("Target GV: --", skin, "accent");
        targetVitality.setName("analysis-target-gv");
        heading.add(targetVitality).top().padRight(20);
        selectedLabel = paragraph("No rate list selected.");
        selectedLabel.setName("analysis-selected");
        heading.add(selectedLabel).growX().top();
        add(heading).row();
        add(header).row();
        HorizontalGroup actions = new HorizontalGroup().wrap().left().rowLeft().space(8).wrapSpace(8);
        actions.addActor(button("SELECT DATA", "analysis-select", this::chooseRates));
        importLists = button("IMPORT LISTS", "analysis-import", this::importRates);
        actions.addActor(importLists);
        analyze = button("ANALYZE", "analysis-run", this::analyze);
        actions.addActor(analyze);
        stop = button("STOP", "analysis-stop", this::stop);
        actions.addActor(stop);
        gvNext = button("GV (NEXT)", "analysis-gv-next", () -> checkVitality(false));
        actions.addActor(gvNext);
        gvAll = button("GV TOP 20", "analysis-gv-all", () -> checkVitality(true));
        actions.addActor(gvAll);
        copy = button("COPY RESULTS", "analysis-copy", () -> Gdx.app.getClipboard().setContents(result.asText()));
        actions.addActor(copy);
        save = button("SAVE TO CURRENT CASE", "analysis-save-case", () -> saveToCase.accept(result));
        actions.addActor(save);
        actions.addActor(button("CLEAR", "analysis-clear", this::clearResult));
        commands.add(actions).row();
        status = paragraph("Choose a list and acquire hotbits before starting.");
        status.setName("analysis-status");
        information.add(status).row();
        summary = paragraph("");
        summary.setName("analysis-summary");
        information.add(summary).row();
        arrangeHeader(false);
        filter = new TextField("", skin);
        filter.setName("analysis-filter");
        filter.setMessageText("Filter results by rate, classification, or subfamily...");
        filter.setProgrammaticChangeEvents(true);
        filter.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                rebuildResults();
            }
        });
        add(filter).minHeight(42).row();
        results.top().left();
        ScrollPane resultColumns = new ScrollPane(results, skin);
        resultColumns.setName("analysis-result-columns");
        resultColumns.setScrollingDisabled(false, true);
        resultColumns.setFadeScrollBars(false);
        resultColumns.setOverscroll(false, false);
        add(resultColumns).minHeight(com.badlogic.gdx.scenes.scene2d.ui.Value.prefHeight).row();
        String starter = Gdx.files.internal("analysis/ANALYSIS_TIME.txt").readString("UTF-8");
        runJob(() -> {
            rates.ensureStarter(starter);
            List<RateRepository.ListInfo> lists = rates.list();
            return lists.isEmpty() ? null : lists.getFirst().id();
        }, id -> {
            selected = id;
            selectedLabel.setText(id == null ? "Import a .txt or .tsv rate list." : id);
            runJob(store::load, restored -> {
                if (restored != null) {
                    showResult(restored, null);
                    status.setText("Restored the last completed analysis.");
                } else {
                    status.setText("Ready. Choose a rate list and acquire hotbits before analyzing.");
                }
            });
        });
    }

    private void analyze() {
        String list = selected;
        String context = caseId.get();
        boolean high = settings.isEnabled(Setting.HIGH_MAX_HIT);
        AnalysisResult previous = context.equals(resultCaseId) ? result : null;
        runJob(() -> {
            AnalysisResult completed = engine.analyze(list, rates.read(list), high, previous);
            persistCompleted(completed);
            return completed;
        }, completed -> {
            showResult(completed, context);
            status.setText("Analysis complete. Check GV or save a snapshot to the current case.");
        });
    }

    private void clearResult() {
        runJob(() -> {
            store.clear();
            return Boolean.TRUE;
        }, ignored -> {
            result = null;
            rebuildResults();
            status.setText("Analysis cleared. Saved case snapshots are unchanged.");
        });
    }

    private void checkVitality(boolean all) {
        AnalysisResult snapshot = result;
        runJob(() -> {
            AnalysisResult checked = engine.checkVitality(snapshot, all);
            persistCompleted(checked);
            return checked;
        }, checked -> {
            showResult(checked, resultCaseId);
            status.setText(all ? "Target and top 20 GV checked." : "Next GV checked (top 20). After the last eligible rate, the next click starts a new pass.");
        });
    }

    public void showResult(AnalysisResult snapshot) {
        showResult(snapshot, caseId.get());
    }

    private void showResult(AnalysisResult snapshot, String context) {
        if (busy) {
            status.setText("Stop or finish the current analysis before opening history.");
            return;
        }
        // Older history remains intact on disk; the working view uses the current 20-rate limit.
        result = new AnalysisResult(snapshot.id(), snapshot.created(), snapshot.database(), snapshot.hitLimit(),
                snapshot.trials(), snapshot.generalVitality(),
                snapshot.rows().subList(0, Math.min(AnalysisEngine.RESULT_LIMIT, snapshot.rows().size())));
        resultCaseId = context;
        rebuildResults();
        update();
    }

    private void chooseRates() {
        runJob(rates::list, lists -> {
            status.setText("Choose a rate list.");
            Dialog dialog = new Dialog("Select rate list", skin) {
                private float previousWidth = -1;
                private float previousHeight = -1;

                @Override
                public void act(float delta) {
                    super.act(delta);
                    if (getStage() != null && (previousWidth != stage.getWidth() || previousHeight != stage.getHeight())) {
                        previousWidth = stage.getWidth();
                        previousHeight = stage.getHeight();
                        setSize(Math.min(680, previousWidth - 48), Math.min(600, previousHeight - 64));
                        setPosition((previousWidth - getWidth()) / 2, (previousHeight - getHeight()) / 2);
                        invalidateHierarchy();
                    }
                }
            };
            dialog.setName("analysis-list-dialog");
            Table choices = new Table();
            choices.top().left();
            TextField search = new TextField("", skin);
            search.setName("analysis-list-search");
            search.setMessageText("Search rate lists...");
            search.setProgrammaticChangeEvents(true);
            Runnable rebuild = () -> {
                choices.clearChildren();
                String query = search.getText().strip().toLowerCase(Locale.ROOT);
                for (RateRepository.ListInfo list : lists) {
                    if (!list.name().toLowerCase(Locale.ROOT).contains(query)) {
                        continue;
                    }
                    TextButton choice = new TextButton(list.name(), skin);
                    choice.setName("analysis-list-" + list.id());
                    choice.getLabel().setWrap(true);
                    choice.pad(12);
                    choice.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            selected = list.id();
                            selectedLabel.setText(list.name());
                            status.setText("Rate list selected. ANALYZE uses acquired hotbits.");
                            dialog.hide();
                        }
                    });
                    choices.add(choice).growX().padBottom(8).row();
                }
                if (choices.getChildren().isEmpty()) {
                    choices.add(paragraph("No rate lists match. Import a list or clear your search.")).growX();
                }
            };
            search.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    rebuild.run();
                }
            });
            rebuild.run();
            ScrollPane scroll = new ScrollPane(choices, skin);
            scroll.setScrollingDisabled(true, false);
            scroll.setFadeScrollBars(false);
            scroll.setName("analysis-list-results");
            scroll.setOverscroll(false, false);
            dialog.getContentTable().add(search).growX().pad(12).row();
            dialog.getContentTable().add(scroll).grow().minSize(0).pad(12);
            TextButton cancel = new TextButton("CANCEL", skin);
            cancel.pad(8, 12, 8, 12);
            dialog.button(cancel);
            dialog.key(Input.Keys.ESCAPE, null);
            dialog.show(stage);
            stage.setKeyboardFocus(search);
            stage.setScrollFocus(scroll);
        });
    }

    private void importRates() {
        filePicker.choose(paths -> {
            if (disposed || paths.isEmpty()) {
                return;
            }
            runJob(() -> {
                RateRepository.ListInfo last = null;
                for (var path : paths) {
                    last = rates.importFile(path);
                }
                return last;
            }, imported -> {
                selected = imported.id();
                selectedLabel.setText(imported.name());
                status.setText("Rate files copied into this application's rates folder.");
            });
        }, message -> status.setText(message));
    }

    private void rebuildResults() {
        results.clearChildren();
        targetVitality.setText("Target GV: " + (result == null || result.generalVitality() == null
                ? "--" : result.generalVitality()));
        if (result == null) {
            summary.setText("No analysis result yet.");
            return;
        }
        String query = filter.getText().strip().toLowerCase(Locale.ROOT);
        String[] headings = {"#", "EV", "RATE", "POTENCY", "LEVEL", "GV", "REC.", "CLASSIFICATION", "SUBFAMILY"};
        for (int column = 0; column < headings.length; column++) {
            addResultCell(headings[column], "352843", column == 2, false).setColor(Color.valueOf("C9B0F5"));
        }
        results.row();
        int shown = 0;
        for (int index = 0; index < result.rows().size(); index++) {
            AnalysisResult.Row row = result.rows().get(index);
            if (!(row.name() + " " + row.classification() + " " + row.subfamily()).toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            shown++;
            String background = shown % 2 == 0 ? "211C2D" : "292238";
            addResultCell(Integer.toString(index + 1), background, false, true);
            addResultCell(Integer.toString(row.energeticValue()), background, false, true);
            Label name = addResultCell(row.name(), background, true, false);
            name.setName("analysis-result-" + index);
            if (safeUrl(row.url())) {
                name.setColor(Color.valueOf("89C9F0"));
                name.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        Gdx.net.openURI(row.url());
                    }
                });
            }
            addResultCell(row.potency(), background, false, false);
            addResultCell(Integer.toString(row.level()), background, false, true);
            Label vitality = addResultCell(row.generalVitality() == null ? "--" : row.generalVitality().toString(),
                    background, false, true);
            if (row.generalVitality() != null && result.generalVitality() != null
                    && row.generalVitality() >= result.generalVitality()) {
                vitality.setColor(Color.valueOf("85DFAE"));
            }
            addResultCell(Integer.toString(row.recurring()), background, false, true);
            addResultCell(row.classification(), background, false, false);
            addResultCell(row.subfamily(), background, false, false);
            results.row();
        }
        summary.setText(shown + " / " + result.rows().size() + " rates | Trials: "
                + result.trials() + " | Hit limit: " + result.hitLimit());
        if (shown == 0) {
            results.add(paragraph("No rates match the filter.")).colspan(headings.length).growX();
        }
    }

    /** Shared table columns keep every result on one compact line, without card padding. */
    private Label addResultCell(String text, String background, boolean expand, boolean numeric) {
        var drawable = skin.newDrawable("white", Color.valueOf(background));
        drawable.setLeftWidth(7);
        drawable.setRightWidth(7);
        drawable.setTopHeight(3);
        drawable.setBottomHeight(3);
        Label.LabelStyle style = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        style.background = drawable;
        Label label = new Label(text, style);
        label.setAlignment(numeric ? Align.right : Align.left);
        var cell = results.add(label).fill().minWidth(com.badlogic.gdx.scenes.scene2d.ui.Value.prefWidth);
        if (expand) {
            cell.expandX();
        }
        return label;
    }

    private boolean safeUrl(String text) {
        try {
            URI uri = URI.create(text);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private <T> void runJob(Callable<T> task, Consumer<T> success) {
        if (busy || disposed) {
            return;
        }
        busy = true;
        drawsUsed = 0;
        calculationProgress = "Working...";
        synchronized (completionLock) {
            cancelled = false;
            committed = false;
        }
        progress = "Working...";
        update();
        worker.execute(() -> {
            activeThread = Thread.currentThread();
            try {
                T value = task.call();
                synchronized (completionLock) {
                    if (cancelled && !committed) {
                        throw new InterruptedIOException("Analysis cancelled.");
                    }
                }
                Gdx.app.postRunnable(() -> {
                    if (!disposed) {
                        busy = false;
                        success.accept(value);
                        update();
                        consumed.run();
                    }
                });
            } catch (Exception exception) {
                boolean wasCancelled = cancelled || exception instanceof InterruptedIOException;
                Gdx.app.postRunnable(() -> {
                    if (!disposed) {
                        busy = false;
                        status.setText(wasCancelled ? "Cancelled. Previous completed results are kept."
                                : "Analysis operation failed after " + drawsUsed + " draws: " + exception.getMessage());
                        update();
                        consumed.run();
                    }
                });
            } finally {
                activeThread = null;
                Thread.interrupted();
            }
        });
    }

    private void stop() {
        synchronized (completionLock) {
            if (committed) {
                return;
            }
            cancelled = true;
            Thread thread = activeThread;
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    private void persistCompleted(AnalysisResult completed) throws java.io.IOException {
        // Cancellation and committing are mutually exclusive: a cancelled result cannot replace recovery data.
        synchronized (completionLock) {
            if (cancelled || Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("Analysis cancelled.");
            }
            store.save(completed);
            committed = true;
        }
    }

    public void update() {
        controls.forEach(control -> control.setDisabled(busy));
        stop.setDisabled(!busy);
        analyze.setDisabled(busy || selected == null);
        importLists.setDisabled(busy || filePicker == null);
        gvNext.setDisabled(busy || result == null);
        gvAll.setDisabled(busy || result == null);
        copy.setDisabled(busy || result == null);
        save.setDisabled(busy || result == null);
        if (busy) {
            status.setText(progress);
        }
    }

    @Override
    public void layout() {
        boolean wide = getWidth() >= 1650f * settings.getFontSize() / 18f;
        if (wide != wideHeader) {
            arrangeHeader(wide);
        }
        super.layout();
    }

    private void arrangeHeader(boolean wide) {
        wideHeader = wide;
        header.clearChildren();
        if (wide) {
            header.add(commands).width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(0.64f, header)).top().left();
            header.add(information).growX().minWidth(0).top().padLeft(24);
        } else {
            header.add(commands).growX().row();
            header.add(information).growX();
        }
        invalidateHierarchy();
    }

    public void triggerAnalysis() {
        if (!analyze.isDisabled()) {
            analyze();
        }
    }

    public void triggerVitality() {
        if (!gvNext.isDisabled()) {
            checkVitality(false);
        }
    }

    public boolean allowsShortcuts() {
        if (stage.getKeyboardFocus() instanceof TextField && stage.getKeyboardFocus() != filter) {
            return false;
        }
        for (Actor actor : stage.getActors()) {
            if (actor instanceof Dialog && actor.isVisible()) {
                return false;
            }
        }
        return true;
    }

    public void dispose() {
        disposed = true;
        stop();
        worker.shutdown();
        try {
            worker.awaitTermination(2, TimeUnit.SECONDS);
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
        controls.add(button);
        return button;
    }

    private Label paragraph(String text) {
        Label label = new Label(text, skin);
        label.setWrap(true);
        return label;
    }
}
