package de.isuret.polos.AetherOnePi.server.streamdeck;

import de.isuret.polos.AetherOnePi.domain.AnalysisResult;
import de.isuret.polos.AetherOnePi.domain.RateObject;
import de.isuret.polos.AetherOnePi.processing2.AetherOneConstants;
import de.isuret.polos.AetherOnePi.processing2.AetherOneUI;
import de.isuret.polos.AetherOnePi.processing2.elements.BroadcastElement;
import de.isuret.polos.AetherOnePi.processing2.elements.SettingsScreen;
import de.isuret.polos.AetherOnePi.server.WebSocketManager;
import de.isuret.polos.AetherOnePi.server.streamdeck.StreamDeckProtocol.Request;
import de.isuret.polos.AetherOnePi.utils.AetherOnePiProcessingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class StreamDeckRuntime {
    public record Analysis(boolean running, int lastResultCount) {}
    public record Vitality(boolean available, Integer value) {}
    public record Broadcast(boolean available, Boolean active, int durationSeconds, Long remainingSeconds,
                            Integer progressPercent, String status, boolean canStart) {}
    public record Rates(String selectedLabel, String candidateLabel) {}
    public record Hotbits(boolean available, String status) {}
    public record Snapshot(Analysis analysis, Vitality gv, Broadcast broadcast, Rates rates, Hotbits hotbits) {}

    private static final Logger LOG = LoggerFactory.getLogger(StreamDeckRuntime.class);
    private static final int DEFAULT_SECONDS = 60;
    private final StreamDeckCommands commands = new StreamDeckCommands();
    private volatile Snapshot snapshot;
    private volatile long lastFrame;
    private AnalysisResult currentResult;
    private int candidate;
    private RateObject selected;
    private int durationSeconds = DEFAULT_SECONDS;

    Snapshot snapshot() {
        return snapshot;
    }

    StreamDeckCommands commands() {
        return commands;
    }

    boolean animationReady() {
        return System.nanoTime() - lastFrame < TimeUnit.SECONDS.toNanos(3);
    }

    /** Called on the Processing animation thread. */
    public void update(AetherOneUI ui) {
        lastFrame = System.nanoTime();
        List<RateObject> rates = rates(ui);
        Request request = commands.poll();
        if (request != null) {
            String code;
            try {
                code = execute(ui, rates, request);
            } catch (Exception e) {
                LOG.error("Stream Deck command failed: {}", request.command(), e);
                code = "operation_failed";
            }
            commands.complete(request, code);
            rates = rates(ui);
        }
        publish(ui, rates, false);
    }

    private List<RateObject> rates(AetherOneUI ui) {
        AnalysisResult result = ui.getAnalysisResult();
        if (currentResult != result) {
            currentResult = result;
            candidate = 0;
            selected = null;
        }
        List<RateObject> rates = result.getRateObjects();
        if (candidate >= rates.size()) {
            candidate = 0;
        }
        if (selected != null && !rates.contains(selected)) {
            selected = null;
        }
        return rates;
    }

    private String execute(AetherOneUI ui, List<RateObject> rates, Request request) throws Exception {
        System.out.println(request.command() + " " + request.args());
        switch (request.command()) {
            case "analysis.show" -> showAnalysisTab(ui);
            case "analysis.start" -> {
                showAnalysisTab(ui);
                publish(ui, rates, true);
                return ui.getAetherOneEventHandler().analyzeCurrentDatabase() ? null : "analysis_failed";
            }
            case "analysis.clear" -> {
                ui.setAnalysisResult(new AnalysisResult());
                ui.setGvCounter(0);
                ui.setGeneralVitality(0);
                WebSocketManager.updateAnalysis();
            }
            case "gv.measure" -> ui.getAetherOneEventHandler().checkGeneralVitality();
            case "broadcast.start" -> {
                String signature = broadcastSignature(ui);
                if (signature == null || signature.isBlank()) {
                    return "no_broadcast_signature";
                }
                var settings = AetherOnePiProcessingConfiguration.loadSettings(AetherOnePiProcessingConfiguration.SETTINGS);
                if (!canStart(ui)) {
                    return "broadcast_unavailable";
                }
                ui.getAetherOneEventHandler().broadcastNow(signature, durationSeconds, settings);
            }
            case "broadcast.stop" -> ui.getGuiElements().stopAllBroadcasts();
            case "broadcast.adjustDuration" ->
                    durationSeconds = Math.clamp(durationSeconds + request.args().get("deltaSteps") * 10, 10, 3600);
            case "broadcast.resetDuration" -> durationSeconds = DEFAULT_SECONDS;
            case "rates.next", "rates.previous", "rates.select" -> {
                if (rates.isEmpty()) {
                    return "no_analysis_results";
                }
                switch (request.command()) {
                    case "rates.next" -> candidate = Math.floorMod(
                            candidate + request.args().getOrDefault("deltaSteps", 1), rates.size());
                    case "rates.previous" -> candidate = Math.floorMod(candidate - 1, rates.size());
                    default -> selected = rates.get(candidate);
                }
            }
            default -> throw new IllegalArgumentException("Unknown Stream Deck command");
        }
        return null;
    }

    private void showAnalysisTab(AetherOneUI ui) {
        ui.getGuiElements().getCp5().getTab(AetherOneConstants.ANALYZE).bringToFront();
        ui.getGuiElements().selectCurrentTab(AetherOneConstants.ANALYZE);
    }

    private String broadcastSignature(AetherOneUI ui) {
        return selected == null ? ui.getAetherOneEventHandler().getBroadcastSignature() : selected.getNameOrRate();
    }

    private boolean canStart(AetherOneUI ui) {
        return !Boolean.TRUE.equals(ui.getAutoMode())
                && ui.getSettings().getBoolean(SettingsScreen.SAFETY_SWITCH, true)
                && activeBroadcast(ui) == null;
    }

    private BroadcastElement activeBroadcast(AetherOneUI ui) {
        for (var element : ui.getGuiElements().getDrawableElementList()) {
            if (element instanceof BroadcastElement broadcast && !broadcast.isStop()) {
                return broadcast;
            }
        }
        return null;
    }

    private void publish(AetherOneUI ui, List<RateObject> rates, boolean running) {
        Integer gv = ui.getGeneralVitality();
        boolean embedded = ui.getSettings().getBoolean(SettingsScreen.BROADCAST_EMBEDDED, true);
        BroadcastElement active = activeBroadcast(ui);
        boolean queued = !ui.getGuiElements().getBroadcastQueueList().isEmpty();
        Long remaining = null;
        Integer progress = null;
        if (active != null && active.getStart() != null && active.getSeconds() > 0) {
            long total = active.getSeconds() * 1000L;
            long elapsed = Math.clamp(System.currentTimeMillis() - active.getStart(), 0, total);
            remaining = (total - elapsed + 999) / 1000;
            progress = (int) (elapsed * 100 / total);
        }
        String status = active != null ? "ACTIVE" : queued ? "QUEUED" : embedded ? "IDLE" : "WINDOW MODE";
        var hotbits = ui.getHotbitsHandler();
        String trng = hotbits.isESP32running() ? "ESP32" : hotbits.isInfiniteUSBrunning() ? "INFINITE USB" : null;
        String signature = broadcastSignature(ui);
        snapshot = new Snapshot(new Analysis(running, rates.size()),
                new Vitality(gv != null && gv > 0, gv != null && gv > 0 ? gv : null),
                new Broadcast(true, embedded || active != null || queued ? active != null || queued : null,
                        durationSeconds, remaining, progress, status,
                        signature != null && !signature.isBlank() && canStart(ui)),
                new Rates(selected == null ? null : selected.getNameOrRate(),
                        rates.isEmpty() ? null : rates.get(candidate).getNameOrRate()),
                new Hotbits(trng != null, trng));
    }
}
