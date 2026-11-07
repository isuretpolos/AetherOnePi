package de.isuret.polos.AetherOnePi.server.streamdeck;

import com.fasterxml.jackson.databind.JsonNode;
import de.isuret.polos.AetherOnePi.server.streamdeck.StreamDeckProtocol.Request;
import de.isuret.polos.AetherOnePi.server.streamdeck.StreamDeckProtocol.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StreamDeckServerTest {
    private static final String PREFIX = "/aopi/api/streamdeck";
    @TempDir
    Path directory;

    @Test
    void discoversMenusAndAssetsFromOneUrl() throws Exception {
        try (var server = start(null)) {
            var bootstrap = json(get(server, PREFIX + "-init"));
            assertEquals("aopi-streamdeck/1", bootstrap.path("protocol").asText());
            URI url = uri(server, PREFIX + "-init");
            var menuUrl = url.resolve(bootstrap.path("endpoints").path("menu").asText().replace("{menuId}", "broadcast"));
            var menu = json(send(HttpRequest.newBuilder(URI.create(menuUrl + "?device=plus")).build()));
            assertEquals("broadcast", menu.path("id").asText());
            assertEquals(bootstrap.path("revision"), menu.path("revision"));
            assertTrue(menu.path("encoders").size() > 0);
            assertEquals(0, json(get(server, PREFIX + "/menus/broadcast?device=key-only")).path("encoders").size());
            var icon = url.resolve(bootstrap.path("assetsBaseUrl").asText()).resolve("broadcast.svg");
            var image = send(HttpRequest.newBuilder(icon).build());
            assertEquals(200, image.statusCode());
            assertEquals("image/svg+xml", image.headers().firstValue("Content-Type").orElseThrow());
            String etag = image.headers().firstValue("ETag").orElseThrow();
            assertEquals(304, send(HttpRequest.newBuilder(icon).header("If-None-Match", etag).build()).statusCode());
            assertEquals(404, get(server, PREFIX + "/assets/..%2Fbroadcast.svg").statusCode());
            var missingMenu = get(server, PREFIX + "/menus/missing");
            assertEquals(404, missingMenu.statusCode());
            assertEquals("menu_not_found", StreamDeckCatalog.JSON.readTree(missingMenu.body()).path("code").asText());
            assertEquals(503, get(server, PREFIX + "/state").statusCode());
        }
    }

    @Test
    void requiresOptInAndAuthenticationAndBlocksBrowserOrigins() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new StreamDeckServer(new StreamDeckRuntime(), directory,
                InetAddress.getByName("0.0.0.0"), 0, true, "short"));
        String token = UUID.randomUUID().toString();
        assertThrows(IllegalArgumentException.class, () -> new StreamDeckServer(new StreamDeckRuntime(), directory,
                InetAddress.getByName("0.0.0.0"), 0, false, token));
        try (var server = start(token)) {
            var url = uri(server, PREFIX + "/health");
            assertEquals(401, get(server, PREFIX + "/health").statusCode());
            assertEquals(403, send(HttpRequest.newBuilder(url).header("Origin", "https://example.org")
                    .header("Authorization", "Bearer " + token).build()).statusCode());
            assertEquals(200, send(HttpRequest.newBuilder(url).header("Authorization", "Bearer " + token)
                    .build()).statusCode());
        }
    }

    @Test
    void validatesAssetsAndReloadsOnlyValidMenuEdits() throws Exception {
        assertFalse(StreamDeckImages.valid("icon.svg", """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 112 112"><script>alert(1)</script></svg>
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var catalog = new StreamDeckCatalog(directory);
        var original = catalog.current();
        assertEquals(StreamDeckCatalog.IDS, original.menus().keySet());
        Files.createDirectories(directory.resolve("menus"));
        Path menu = directory.resolve("menus/main.json");
        var source = getClass().getResourceAsStream("/streamdeck/menus/main.json");
        assertNotNull(source);
        try (source) {
            Files.writeString(menu, new String(source.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .replace("AetherOnePi", "Updated"));
        }
        var variant = (com.fasterxml.jackson.databind.node.ObjectNode) StreamDeckCatalog.JSON.readTree(Files.readString(menu));
        variant.withArray("keys").addObject().put("slot", 35).put("title", "Extra").put("enabled", false);
        Files.writeString(directory.resolve("menus/main.plus-xl.json"), variant.toString());
        Thread.sleep(1100);
        var updated = catalog.current();
        assertNotEquals(original.revision(), updated.revision());
        assertEquals("Updated", updated.menus().get("main").title());
        assertEquals(35, updated.variants().get("main.plus-xl").keys()[6].slot());
        Files.writeString(menu, "{invalid");
        Thread.sleep(1100);
        assertEquals(updated.revision(), catalog.current().revision());
    }

    @Test
    void deduplicatesRequestsAndGivesStopPriority() {
        var commands = new StreamDeckCommands();
        Request start = command("broadcast.start", Map.of(), 0);
        assertEquals(503, commands.submit(start, false).status());
        assertEquals(202, commands.submit(start, true).status());
        assertEquals(202, commands.submit(start, false).status());
        Request stop = command("broadcast.stop", Map.of(), 1);
        assertEquals(202, commands.submit(stop, false).status());
        assertEquals(stop, commands.poll());
        assertNull(commands.poll());
        assertEquals("stopped_before_start", commands.submit(start, true).job().code());

        Request clear = command("analysis.clear", Map.of(), 0);
        assertEquals(202, commands.submit(clear, true).status());
        assertEquals(clear, commands.poll());
        commands.complete(clear, null);
        assertEquals("completed", commands.submit(clear, true).job().status());
        assertNull(commands.poll());
        assertEquals(409, commands.submit(new Request(clear.requestId(), stop.command(), stop.args(), stop.source()), true)
                .status());
        assertEquals(400, commands.submit(command("broadcast.adjustDuration", Map.of("deltaSteps", 21), 0), true).status());
        assertEquals(400, commands.submit(command("analysis.clear", Map.of(), 36), true).status());
        assertEquals(404, commands.submit(command("unknown", Map.of(), 0), true).status());
    }

    private StreamDeckServer start(String token) throws IOException {
        return new StreamDeckServer(new StreamDeckRuntime(), directory, InetAddress.getByName("127.0.0.1"),
                0, false, token);
    }

    private static Request command(String command, Map<String, Integer> args, int slot) {
        return new Request(UUID.randomUUID().toString(), command, args, new Source("device", "broadcast", slot));
    }

    private static URI uri(StreamDeckServer server, String path) {
        return URI.create("http://127.0.0.1:" + server.port() + path);
    }

    private static HttpResponse<String> get(StreamDeckServer server, String path) throws Exception {
        return send(HttpRequest.newBuilder(uri(server, path)).build());
    }

    private static HttpResponse<String> send(HttpRequest request) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private static JsonNode json(HttpResponse<String> response) throws IOException {
        assertEquals(200, response.statusCode(), response.body());
        return StreamDeckCatalog.JSON.readTree(response.body());
    }
}
