package de.isuret.polos.AetherOnePi.server.streamdeck;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Set;

final class StreamDeckProtocol {
    static final String VERSION = "aopi-streamdeck/1";
    static final Set<String> COMMANDS = Set.of("analysis.start", "analysis.clear", "gv.measure",
            "broadcast.start", "broadcast.stop", "broadcast.adjustDuration", "broadcast.resetDuration",
            "rates.next", "rates.previous", "rates.select");
    static final Set<String> STATE_KEYS = Set.of("analysis.lastResultCount", "gv.value", "broadcast.durationSeconds",
            "broadcast.progressPercent", "broadcast.status", "rates.selectedLabel", "rates.candidateLabel",
            "hotbits.status");
    static final Set<String> LAYOUTS = Set.of("value-dial", "status-dial", "progress-dial", "navigation-dial");

    record Interaction(String type, String menuId, String command) {}
    record Key(@JsonProperty(required = true) int slot, String title, String icon,
               @JsonProperty(required = true) boolean enabled, String valueKey, Interaction press) {}
    record Encoder(@JsonProperty(required = true) int slot, String title, String layout, String valueKey, String unit,
                   String rotateCommand, Interaction push, Interaction touch, Interaction longTouch) {}
    record Menu(String id, String title, Key[] keys, Encoder[] encoders) {}
    record Source(String deviceId, String menuId, @JsonProperty(required = true) int slot) {}
    record Request(String requestId, String command, Map<String, Integer> args, Source source) {}
    record Job(String requestId, String status, String code) {}

    private StreamDeckProtocol() {}
}
