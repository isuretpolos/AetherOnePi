package de.isuret.polos.aopi.broadcast;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Render-thread queue driven by monotonic time, independent of frames and tab visibility. */
public class BroadcastService {

    public record Request(String signature, int seconds, int intensity) {
        public Request {
            signature = signature.strip();
            if (signature.isEmpty() || signature.length() > 20000 || seconds < 1 || seconds > 86400
                    || intensity < 1 || intensity > 1000) {
                throw new IllegalArgumentException("Enter a signature, 1–86400 seconds, and intensity 1–1000.");
            }
        }
    }

    public record Report(String id, String signature, int seconds, int intensity, Instant started,
                         Instant ended, double elapsedSeconds, String outcome) {
        public String asText() {
            return "Broadcast: " + outcome + "\n" + signature + "\nStarted: " + started
                    + "\nEnded: " + ended + "\nDuration: " + seconds + " seconds | Elapsed: "
                    + Math.round(elapsedSeconds) + " seconds | Multiplier: " + intensity;
        }
    }

    private final LongSupplier clock;
    private final ArrayDeque<Request> queue = new ArrayDeque<>();
    private final List<Report> reports = new ArrayList<>();
    private boolean running;
    public record Unit(String id, Request request, long startedNanos, Instant started) {
    }

    private final List<Unit> active = new ArrayList<>();
    private int concurrency = 8;
    private final java.util.Map<String, Integer> resonances = new java.util.LinkedHashMap<>();
    public record ResonanceEvent(Instant time, String signature) { }
    private final List<ResonanceEvent> resonanceEvents = new ArrayList<>();
    private final String protocolId = UUID.randomUUID().toString();

    public void setSingleRate(boolean single) {
        concurrency = single ? 1 : 8;
    }

    public List<Unit> active() {
        return List.copyOf(active);
    }

    public double elapsedSeconds(Unit unit) {
        return Math.max(0, (clock.getAsLong() - unit.startedNanos()) / 1_000_000_000d);
    }

    public void resonate(String signature) {
        resonances.merge(signature, 1, Integer::sum);
        resonanceEvents.add(new ResonanceEvent(Instant.now(), signature));
    }

    public java.util.Map<String, Integer> resonances() {
        return java.util.Map.copyOf(resonances);
    }

    public List<ResonanceEvent> resonanceEvents() {
        return List.copyOf(resonanceEvents);
    }

    public String protocolId() {
        return protocolId;
    }

    public BroadcastService() {
        this(System::nanoTime);
    }

    public BroadcastService(LongSupplier clock) {
        this.clock = clock;
    }

    public void enqueue(List<Request> requests) {
        if (requests.isEmpty() || queue.size() + requests.size() > 500) {
            throw new IllegalArgumentException("Queue between 1 and 500 broadcasts.");
        }
        queue.addAll(List.copyOf(requests));
    }

    public void start() {
        if (!queue.isEmpty()) {
            running = true;
            fillSlots(clock.getAsLong());
        }
    }

    private void fillSlots(long now) {
        while (running && active.size() < concurrency && !queue.isEmpty()) {
            active.add(new Unit(UUID.randomUUID().toString(), queue.removeFirst(), now, Instant.now()));
        }
        running = !active.isEmpty();
    }

    public void update() {
        long now = clock.getAsLong();
        for (Unit unit : List.copyOf(active)) {
            if (elapsedSeconds(unit) >= unit.request().seconds()) {
                finish(unit, "Completed", unit.request().seconds());
                active.remove(unit);
            }
        }
        if (running) {
            fillSlots(now);
        }
    }

    public void stop() {
        for (Unit unit : active) {
            finish(unit, "Stopped", Math.min(unit.request().seconds(), elapsedSeconds(unit)));
        }
        active.clear();
        running = false;
    }

    public void stopCurrent() {
        if (!active.isEmpty()) {
            Unit unit = active.removeFirst();
            finish(unit, "Stopped", Math.min(unit.request().seconds(), elapsedSeconds(unit)));
        }
        if (running) {
            fillSlots(clock.getAsLong());
        }
    }

    public void stopAll() {
        stop();
        clearQueue();
    }

    private void finish(Unit unit, String outcome, double elapsed) {
        reports.add(new Report(UUID.randomUUID().toString(), unit.request().signature(), unit.request().seconds(),
                unit.request().intensity(), unit.started(), Instant.now(), elapsed, outcome));
        if (reports.size() > 200) {
            reports.removeFirst();
        }
    }

    public double elapsedSeconds() {
        return active.isEmpty() ? 0 : elapsedSeconds(active.getFirst());
    }

    public Request current() {
        return active.isEmpty() ? null : active.getFirst().request();
    }

    public boolean isRunning() {
        return running;
    }

    public List<Request> queued() {
        return List.copyOf(queue);
    }

    public void removeQueued(int index) {
        List<Request> requests = new ArrayList<>(queue);
        requests.remove(index);
        queue.clear();
        queue.addAll(requests);
    }

    public void clearQueue() {
        queue.clear();
    }

    public List<Report> reports() {
        return List.copyOf(reports);
    }
}
