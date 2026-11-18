package de.isuret.polos.aopi.broadcast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.isuret.polos.aopi.analysis.AnalysisResult;
import de.isuret.polos.aopi.settings.ApplicationSettings;
import de.isuret.polos.aopi.settings.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Compact desktop broadcast controls. Timing continues in the shell while this page is detached. */
public class BroadcastPage extends Table {

    private final BroadcastService service;
    private final BroadcastHistory history;
    private final ApplicationSettings settings;
    private final Supplier<AnalysisResult> analysis;
    private final Skin skin;
    private final TextArea signature;
    private final TextField seconds;
    private final TextField intensity;
    private final Label status;
    private final Label current;
    private final Label lastReport;
    private final Table controlHeader = new Table();
    private final Table commandArea = new Table();
    private final Table information = new Table();
    private boolean wideControls;
    private final TextButton start;
    private final TextButton stop;
    private final TextButton save;
    private final ExecutorService io = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "aopi-broadcast-history");
        thread.setDaemon(true);
        return thread;
    });
    private String latestId = "";
    private boolean disposed;
    private final BroadcastVisual visual;
    private final Label resonanceText;
    private final java.util.Set<String> persisted = new java.util.HashSet<>();
    private int persistedResonances;

    public BroadcastPage(Skin skin, BroadcastService service, BroadcastHistory history, ApplicationSettings settings,
                         Supplier<AnalysisResult> analysis, Consumer<BroadcastService.Report> saveToCase,
                         Consumer<String> notification, de.isuret.polos.aopi.session.CachedHotbits hotbits,
                         java.util.function.IntSupplier cacheSize, BroadcastImagePicker imagePicker) {
        this.skin = skin;
        this.service = service;
        this.history = history;
        this.settings = settings;
        this.analysis = analysis;
        top().left();
        defaults().growX().spaceBottom(8);
        status = label("Ready.");
        current = label("Idle");
        lastReport = label("");
        signature = new TextArea("", skin);
        signature.setName("broadcast-signature");
        signature.setMessageText("Enter a signature, intention, or rates...");
        signature.setMaxLength(20000);
        seconds = number("60", "broadcast-seconds");
        intensity = number("1", "broadcast-intensity");
        Table inputs = new Table();
        inputs.setName("broadcast-inputs");
        inputs.add(signature).growX().minWidth(0).height(new com.badlogic.gdx.scenes.scene2d.ui.Value() {
            @Override
            public float get(Actor context) {
                return Math.max(42, signature.getStyle().font.getLineHeight() + 20);
            }
        });
        inputs.add(new Label("Seconds", skin)).padLeft(8).padRight(6);
        inputs.add(seconds).width(numberWidth(seconds, 5)).fillY();
        inputs.add(new Label("Multiplier", skin)).padLeft(8).padRight(6);
        inputs.add(intensity).width(numberWidth(intensity, 4)).fillY();
        add(inputs).row();
        commandArea.setName("broadcast-command-area");
        commandArea.top().left();
        commandArea.defaults().growX().spaceBottom(6);
        information.setName("broadcast-information");
        information.top().left();
        information.defaults().growX().spaceBottom(6);
        add(controlHeader).row();
        HorizontalGroup actions = new HorizontalGroup().wrap().left().space(6).wrapSpace(6);
        actions.addActor(button("USE ANALYSIS", "broadcast-use-analysis", () -> {
            AnalysisResult result = analysis.get();
            if (result == null) {
                status.setText("Run or open an analysis first.");
            } else {
                useRates(result.rows().stream().map(AnalysisResult.Row::name).toList());
            }
        }));
        actions.addActor(button("ADD TO QUEUE", "broadcast-add", () -> enqueue(false)));
        start = button("BROADCAST NOW", "broadcast-start", () -> {
            if (service.queued().isEmpty()) {
                enqueue(true);
            } else {
                service.start();
            }
        });
        actions.addActor(start);
        stop = button("STOP CURRENT", "broadcast-stop", () -> {
            service.stopCurrent();
            status.setText("Stopped current broadcast.");
        });
        actions.addActor(stop);
        actions.addActor(button("STOP ALL", "broadcast-stop-all", () -> {
            service.stopAll();
            status.setText("All active and queued broadcasts stopped.");
        }));
        actions.addActor(button("CLEAR QUEUE", "broadcast-clear", () -> {
            service.clearQueue();
            status.setText("Pending queue cleared. Use STOP to stop the current broadcast.");
        }));
        save = button("SAVE REPORT TO CASE", "broadcast-save-case", () -> saveToCase.accept(latest()));
        actions.addActor(save);
        commandArea.add(actions).row();
        HorizontalGroup images = new HorizontalGroup().wrap().left().space(6).wrapSpace(6);
        for (var source : BroadcastImagePicker.Source.values()) {
            String title = switch (source) {
                case FILES -> "ADD IMAGES";
                case LAYERS -> "IMAGE LAYERS";
                case CLIPBOARD -> "PASTE IMAGE";
            };
            TextButton pick = button(title, "broadcast-images-" + source.name(), () ->
                    imagePicker.choose(source, paths -> loadImages(paths, source == BroadcastImagePicker.Source.LAYERS),
                            status::setText));
            pick.setDisabled(imagePicker == null);
            images.addActor(pick);
        }
        images.addActor(button("CLEAR IMAGES", "broadcast-images-clear", this::clearImages));
        commandArea.add(images).row();
        status.setName("broadcast-status");
        information.add(status).row();
        current.setName("broadcast-current");
        information.add(current).row();
        arrangeControls(false);
        visual = new BroadcastVisual(service, new BroadcastContext(skin, settings, hotbits, cacheSize), skin);
        add(visual).minHeight(com.badlogic.gdx.scenes.scene2d.ui.Value.prefHeight).row();
        resonanceText = label("RESONATED RATES");
        resonanceText.setName("broadcast-resonances");
        add(resonanceText).row();
        lastReport.setName("broadcast-report");
        add(lastReport).row();
        update(notification);
    }

    public void useRates(List<String> rates) {
        signature.setText(String.join("\n", rates));
        status.setText("Analysis rates copied. ADD TO QUEUE or START when ready.");
    }

    private void enqueue(boolean run) {
        try {
            int duration = Integer.parseInt(seconds.getText());
            int strength = Integer.parseInt(intensity.getText());
            if (duration < 1 || duration > 86400 || strength < 1 || strength > 1000) {
                throw new IllegalArgumentException("Use 1–86400 seconds and intensity 1–1000.");
            }
            if (settings.isEnabled(Setting.BROADCAST_DELTA_TIME)) {
                AnalysisResult result = analysis.get();
                if (result != null && result.generalVitality() != null && result.generalVitality() > 0
                        && result.generalVitality() < duration) {
                    duration -= result.generalVitality();
                }
            }
            List<String> signatures = signature.getText().lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
            List<BroadcastService.Request> requests = new ArrayList<>();
            if (signatures.size() > 500) {
                throw new IllegalArgumentException("The queue supports at most 500 items.");
            }
            for (String value : signatures) {
                requests.add(new BroadcastService.Request(value, duration, strength));
            }
            service.enqueue(requests);
            if (run) {
                service.start();
            }
            status.setText("Queued " + requests.size() + " broadcast(s).");
        } catch (IllegalArgumentException exception) {
            status.setText("Cannot queue broadcast: " + exception.getMessage());
        }
    }

    public void update(Consumer<String> notification) {
        service.setSingleRate(settings.isEnabled(Setting.BROADCAST_SINGLE_RATES));
        service.update();
        visual.update();
        var active = service.current();
        start.setDisabled(false);
        stop.setDisabled(!service.isRunning());
        save.setDisabled(latest() == null);
        if (active == null) {
            current.setText("Idle | Queued: " + service.queued().size());
        } else {
            current.setText("Active: " + service.active().size() + " / "
                    + (settings.isEnabled(Setting.BROADCAST_SINGLE_RATES) ? 1 : 8) + " | Queued: " + service.queued().size());
        }
        var report = latest();
        if (report != null && !report.id().equals(latestId)) {
            latestId = report.id();
            lastReport.setText(report.asText());
            if (settings.isEnabled(Setting.NOTIFICATIONS) && report.outcome().equals("Completed")) {
                notification.accept("Broadcast completed: " + report.signature());
            }
        }
        persistNewReports();
        StringBuilder resonated = new StringBuilder("RESONATED RATES\n");
        service.resonances().entrySet().stream().sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10).forEach(entry -> resonated.append(entry.getValue()).append("  ").append(entry.getKey()).append('\n'));
        resonanceText.setText(resonated);
    }

    private BroadcastService.Report latest() {
        var reports = service.reports();
        return reports.isEmpty() ? null : reports.getLast();
    }

    @Override
    public void layout() {
        boolean wide = getWidth() >= 1200 * settings.getFontSize() / 16f;
        if (wide != wideControls) {
            arrangeControls(wide);
        }
        super.layout();
    }

    private void arrangeControls(boolean wide) {
        wideControls = wide;
        controlHeader.clearChildren();
        if (wide) {
            controlHeader.add(commandArea).growX().minWidth(0).top();
            controlHeader.add(information).width(new com.badlogic.gdx.scenes.scene2d.ui.Value() {
                @Override
                public float get(Actor context) {
                    return 280 * settings.getFontSize() / 16f;
                }
            }).top().padLeft(16);
        } else {
            controlHeader.add(commandArea).growX().row();
            controlHeader.add(information).growX();
        }
        invalidateHierarchy();
    }

    private void persist(BroadcastService.Report report) {
        io.execute(() -> {
            try {
                history.save(report);
            } catch (Exception exception) {
                Gdx.app.postRunnable(() -> {
                    if (!disposed) {
                        status.setText("Could not save broadcast history: " + exception.getMessage()
                                + ". The report is still available to save to a case.");
                    }
                });
            }
        });
    }

    public void dispose() {
        service.stop();
        persistNewReports();
        visual.dispose();
        disposed = true;
        io.shutdown();
        try {
            io.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void persistNewReports() {
        for (var report : service.reports()) {
            if (persisted.add(report.id())) {
                persist(report);
            }
        }
        var events = service.resonanceEvents();
        if (events.size() != persistedResonances) {
            persistedResonances = events.size();
            io.execute(() -> {
                try {
                    history.saveResonances(service.protocolId(), events);
                } catch (Exception exception) {
                    Gdx.app.error("Broadcast", "Could not save resonance protocol.", exception);
                }
            });
        }
    }

    private void loadImages(List<java.nio.file.Path> paths, boolean layers) {
        List<BroadcastImage> loaded = new ArrayList<>();
        try {
            List<java.nio.file.Path> files = paths;
            if (paths.size() == 1 && java.nio.file.Files.isDirectory(paths.getFirst())) {
                var folder = paths.getFirst();
                String base = folder.getFileName() + ".png";
                try (var stream = java.nio.file.Files.list(folder)) {
                    files = stream.filter(java.nio.file.Files::isRegularFile)
                            .filter(path -> path.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                            .sorted(java.util.Comparator.<java.nio.file.Path>comparingInt(path ->
                                    path.getFileName().toString().equalsIgnoreCase(base) ? 0 : 1)
                                    .thenComparing(java.nio.file.Path::toString)).toList();
                }
            }
            if (files.isEmpty()) { return; }
            if (files.size() > 64) { throw new IllegalArgumentException("Choose at most 64 images."); }
            for (var path : files) {
                loaded.add(BroadcastImage.load(path));
            }
            visual.context().setImages(loaded, layers);
            status.setText("Loaded " + loaded.size() + (layers ? " image layers. Enable Broadcast image layers in Settings." : " broadcast images."));
        } catch (Exception exception) {
            loaded.forEach(BroadcastImage::dispose);
            status.setText("Could not load images: " + exception.getMessage());
        }
    }

    private void clearImages() {
        visual.context().setImages(List.of(), true);
        visual.context().setImages(List.of(), false);
        status.setText("Broadcast images cleared.");
    }

    private TextField number(String value, String name) {
        TextField field = new TextField(value, skin);
        field.setName(name);
        field.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        field.setMaxLength(5);
        field.setWidth(100);
        return field;
    }

    private com.badlogic.gdx.scenes.scene2d.ui.Value numberWidth(TextField field, int digits) {
        return new com.badlogic.gdx.scenes.scene2d.ui.Value() {
            @Override
            public float get(Actor context) {
                return Math.max(70, field.getStyle().font.getData().getGlyph('0').xadvance * digits + 24);
            }
        };
    }

    private TextButton button(String text, String name, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.setName(name);
        button.pad(7, 10, 7, 10);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    private Label label(String text) {
        Label label = new Label(text, skin);
        label.setWrap(true);
        return label;
    }
}
