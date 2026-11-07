package de.isuret.polos.AetherOnePi.server.streamdeck;

import de.isuret.polos.AetherOnePi.server.streamdeck.StreamDeckProtocol.Job;
import de.isuret.polos.AetherOnePi.server.streamdeck.StreamDeckProtocol.Request;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

final class StreamDeckCommands {
    record Submission(int status, Job job) {}

    private record Entry(Request request, Job job, long expires) {}
    private record Queued(Request request, long deadline) {}

    private final ArrayDeque<Queued> queue = new ArrayDeque<>();
    private final LinkedHashMap<String, Entry> requests = new LinkedHashMap<>();
    private long lastRotation;
    private boolean stopPending;

    synchronized Submission submit(Request request, boolean animationReady) {
        String error = validate(request);
        if (error != null) {
            return reject(request, "unknown_command".equals(error) ? 404 : 400, error);
        }
        long now = System.nanoTime();
        requests.values().removeIf(entry -> entry.expires() <= now && !"queued".equals(entry.job().status()));
        Entry existing = requests.get(request.requestId());
        if (existing != null) {
            if (!existing.request().equals(request)) {
                return reject(request, 409, "request_id_conflict");
            }
            return new Submission("queued".equals(existing.job().status()) ? 202 : 200, existing.job());
        }
        boolean stop = "broadcast.stop".equals(request.command());
        if (!stop && !animationReady) {
            return reject(request, 503, "animation_thread_busy");
        }
        if (stop && stopPending) {
            return reject(request, 409, "stop_already_queued");
        }
        if (!stop && (queue.size() >= 8 || requests.size() >= 1024)) {
            return reject(request, 429, "command_limit");
        }
        if (stop && requests.size() >= 1025) {
            return reject(request, 429, "command_limit");
        }
        if (!request.args().isEmpty()) {
            if (now - lastRotation < TimeUnit.MILLISECONDS.toNanos(100)) {
                return reject(request, 429, "rotation_limit");
            }
            lastRotation = now;
        }
        Job job = new Job(request.requestId(), "queued", null);
        requests.put(request.requestId(), new Entry(request, job, Long.MAX_VALUE));
        if (stop) {
            stopPending = true;
            queue.removeIf(pending -> {
                if ("broadcast.start".equals(pending.request().command())) {
                    complete(pending.request(), "stopped_before_start");
                    return true;
                }
                return false;
            });
            queue.addFirst(new Queued(request, Long.MAX_VALUE));
        } else {
            queue.addLast(new Queued(request, now + TimeUnit.SECONDS.toNanos(5)));
        }
        return new Submission(202, job);
    }

    synchronized Request poll() {
        Queued queued;
        while ((queued = queue.pollFirst()) != null) {
            Request request = queued.request();
            if ("broadcast.stop".equals(request.command())) {
                stopPending = false;
                return request;
            }
            if (System.nanoTime() <= queued.deadline()) {
                return request;
            }
            complete(request, "command_expired");
        }
        return null;
    }

    synchronized void complete(Request request, String code) {
        Job job = new Job(request.requestId(), code == null ? "completed" : "rejected", code);
        requests.put(request.requestId(), new Entry(request, job, System.nanoTime() + TimeUnit.MINUTES.toNanos(5)));
    }

    synchronized List<Job> recentJobs() {
        return requests.values().stream().skip(Math.max(0, requests.size() - 16)).map(Entry::job).toList();
    }

    private static Submission reject(Request request, int status, String code) {
        return new Submission(status, new Job(request == null ? null : request.requestId(), "rejected", code));
    }

    private static String validate(Request request) {
        if (request == null || request.requestId() == null || request.requestId().length() != 36
                || request.command() == null || request.args() == null || request.source() == null) {
            return "invalid_request";
        }
        try {
            if (!UUID.fromString(request.requestId()).toString().equals(request.requestId())) {
                return "invalid_request_id";
            }
        } catch (IllegalArgumentException e) {
            return "invalid_request_id";
        }
        var source = request.source();
        if (source.deviceId() == null || source.deviceId().isBlank() || source.deviceId().length() > 128
                || source.menuId() == null || !StreamDeckCatalog.IDS.contains(source.menuId())
                || source.slot() < 0 || source.slot() >= 36) {
            return "invalid_source";
        }
        if (!StreamDeckProtocol.COMMANDS.contains(request.command())) {
            return "unknown_command";
        }
        if ("broadcast.adjustDuration".equals(request.command())
                || "rates.next".equals(request.command()) && !request.args().isEmpty()) {
            Integer delta = request.args().get("deltaSteps");
            return request.args().size() == 1 && delta != null && delta >= -20 && delta <= 20 && delta != 0
                    ? null : "invalid_arguments";
        }
        return request.args().isEmpty() ? null : "invalid_arguments";
    }
}
