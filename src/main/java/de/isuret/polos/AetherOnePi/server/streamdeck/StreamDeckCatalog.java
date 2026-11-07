package de.isuret.polos.AetherOnePi.server.streamdeck;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import de.isuret.polos.AetherOnePi.server.streamdeck.StreamDeckProtocol.Encoder;
import de.isuret.polos.AetherOnePi.server.streamdeck.StreamDeckProtocol.Interaction;
import de.isuret.polos.AetherOnePi.server.streamdeck.StreamDeckProtocol.Key;
import de.isuret.polos.AetherOnePi.server.streamdeck.StreamDeckProtocol.Menu;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StreamDeckCatalog {
    static final ObjectMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .disable(com.fasterxml.jackson.databind.MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();
    static final Set<String> IDS = Set.of("main", "analysis", "broadcast", "rates", "settings");
    private static final Pattern ASSET = Pattern.compile("[a-zA-Z0-9_-]{1,48}\\.(svg|png)");
    private static final Logger LOG = LoggerFactory.getLogger(StreamDeckCatalog.class);

    record Device(int keys, int encoders) {}
    static final Map<String, Device> DEVICES = Map.of("plus-xl", new Device(36, 6), "plus", new Device(8, 4),
            "xl", new Device(32, 0), "key-only", new Device(6, 0));

    record Catalog(String revision, Map<String, Menu> menus, Map<String, Menu> variants,
                   Map<String, byte[]> assets) {}

    private final Path directory;
    private final Catalog bundled;
    private Catalog current;
    private long lastRefresh;
    private boolean invalid;

    StreamDeckCatalog(Path directory) throws IOException {
        this.directory = directory;
        bundled = load(false);
        current = bundled;
    }

    synchronized Catalog current() {
        long now = System.nanoTime();
        if (now - lastRefresh >= TimeUnit.SECONDS.toNanos(1)) {
            lastRefresh = now;
            try {
                current = Files.isDirectory(directory) ? load(true) : bundled;
                invalid = false;
            } catch (IOException | IllegalArgumentException e) {
                if (!invalid) {
                    LOG.warn("Ignoring invalid Stream Deck configuration: {}", e.getMessage());
                }
                invalid = true;
            }
        }
        return current;
    }

    private Catalog load(boolean overrides) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        Map<String, Menu> menus = new HashMap<>();
        Map<String, Menu> variants = new HashMap<>();
        Map<String, byte[]> assets = new HashMap<>();
        for (String id : IDS.stream().sorted().toList()) {
            menus.put(id, readMenu(id, id, new Device(36, 6), 6, overrides, assets, digest));
            if (overrides) {
                for (String device : DEVICES.keySet().stream().sorted().toList()) {
                    String variant = id + "." + device;
                    if (Files.exists(directory.resolve("menus/" + variant + ".json"))) {
                        Device capacity = DEVICES.get(device);
                        variants.put(variant, readMenu(id, variant, capacity, capacity.keys(), true, assets, digest));
                    }
                }
            }
        }
        return new Catalog(HexFormat.of().formatHex(digest.digest()), Map.copyOf(menus), Map.copyOf(variants),
                Map.copyOf(assets));
    }

    private Menu readMenu(String id, String filename, Device device, int keyLimit, boolean overrides,
                          Map<String, byte[]> assets, MessageDigest digest) throws IOException {
        byte[] bytes = read("menus/" + filename + ".json", 32768, overrides);
        digest.update(filename.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        digest.update(bytes);
        Menu menu = JSON.readValue(bytes, Menu.class);
        require(menu != null && id.equals(menu.id()) && label(menu.title()), "Invalid menu " + filename);
        validate(menu, device, keyLimit);
        for (Key key : menu.keys()) {
            if (key.icon() != null && !assets.containsKey(key.icon())) {
                byte[] image = read("assets/" + key.icon(), 65536, overrides);
                require(StreamDeckImages.valid(key.icon(), image), "Invalid image " + key.icon());
                assets.put(key.icon(), image);
                digest.update(image);
            }
        }
        return menu;
    }

    private byte[] read(String name, int limit, boolean overrides) throws IOException {
        Path file = directory.resolve(name);
        InputStream input;
        if (overrides && Files.exists(file)) {
            if (!file.toRealPath().startsWith(directory.toRealPath())) {
                throw new IOException("Stream Deck override escapes configuration directory");
            }
            input = Files.newInputStream(file);
        } else {
            input = StreamDeckCatalog.class.getResourceAsStream("/streamdeck/" + name);
        }
        if (input == null) {
            throw new IOException("Missing Stream Deck resource " + name);
        }
        try (input) {
            byte[] bytes = input.readNBytes(limit + 1);
            require(bytes.length <= limit, "Oversized resource " + name);
            return bytes;
        }
    }

    static List<Key> compactKeys(Menu menu, int capacity) {
        if (Arrays.stream(menu.keys()).allMatch(key -> key.slot() < capacity)) {
            return List.of(menu.keys());
        }
        List<Key> compact = new ArrayList<>();
        for (Key key : Arrays.stream(menu.keys()).sorted(Comparator.comparingInt(Key::slot)).toList()) {
            compact.add(new Key(compact.size(), key.title(), key.icon(), key.enabled(), key.valueKey(), key.press()));
        }
        return List.copyOf(compact);
    }

    private static void validate(Menu menu, Device device, int keyLimit) {
        require(menu.keys() != null && menu.keys().length <= keyLimit && menu.encoders() != null,
                "Invalid menu size");
        Set<Integer> slots = new HashSet<>();
        boolean exit = false;
        boolean stop = false;
        for (Key key : menu.keys()) {
            require(key != null && key.slot() >= 0 && key.slot() < device.keys() && slots.add(key.slot())
                    && label(key.title()) && (key.icon() == null || ASSET.matcher(key.icon()).matches()),
                    "Invalid key slot");
            validateValue(key.valueKey());
            validate(key.press());
            exit |= key.enabled() && key.press() != null && Set.of("back", "home").contains(key.press().type());
            stop |= key.enabled() && key.press() != null && "broadcast.stop".equals(key.press().command());
        }
        require("main".equals(menu.id()) || exit, "Menu requires an enabled BACK or HOME key");
        require(!"broadcast".equals(menu.id()) || stop, "Broadcast menu requires an enabled STOP key");
        slots.clear();
        for (Encoder encoder : menu.encoders()) {
            require(encoder != null && encoder.slot() >= 0 && encoder.slot() < device.encoders()
                    && slots.add(encoder.slot())
                    && label(encoder.title()) && encoder.layout() != null
                    && StreamDeckProtocol.LAYOUTS.contains(encoder.layout())
                    && (encoder.unit() == null || encoder.unit().length() <= 8)
                    && (encoder.rotateCommand() == null || Set.of("broadcast.adjustDuration", "rates.next")
                    .contains(encoder.rotateCommand())), "Invalid encoder slot");
            validateValue(encoder.valueKey());
            validate(encoder.push());
            validate(encoder.touch());
            validate(encoder.longTouch());
        }
    }

    private static boolean label(String title) {
        return title != null && !title.isBlank() && title.length() <= 40;
    }

    private static void validateValue(String valueKey) {
        require(valueKey == null || StreamDeckProtocol.STATE_KEYS.contains(valueKey), "Invalid state key");
    }

    private static void validate(Interaction action) {
        if (action == null) {
            return;
        }
        require(action.type() != null && switch (action.type()) {
            case "navigate" -> action.menuId() != null && IDS.contains(action.menuId()) && action.command() == null;
            case "command" -> action.command() != null && StreamDeckProtocol.COMMANDS.contains(action.command())
                    && !action.command().equals("broadcast.adjustDuration") && action.menuId() == null;
            case "back", "home", "none" -> action.command() == null && action.menuId() == null;
            default -> false;
        }, "Invalid menu interaction");
    }

    private static void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalArgumentException(message);
        }
    }
}
