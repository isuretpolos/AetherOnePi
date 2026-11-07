package de.isuret.polos.AetherOnePi.server.streamdeck;

import de.isuret.polos.AetherOnePi.server.streamdeck.StreamDeckProtocol.Request;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public final class StreamDeckServer implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(StreamDeckServer.class);
    private static final String PREFIX = "/aopi/api";
    private static final String ROOT = PREFIX + "/streamdeck";
    private static final int MAX_COMMAND_BYTES = 4096;
    private final String instanceId = UUID.randomUUID().toString();
    private final String applicationVersion = readApplicationVersion();
    private final StreamDeckRuntime runtime;
    private final StreamDeckCatalog catalog;
    private final Javalin app;
    private final boolean lan;
    private final byte[] authorization;

    StreamDeckServer(StreamDeckRuntime runtime, Path directory, InetAddress host, int port,
                     boolean lan, String token) throws IOException {
        if (!host.isLoopbackAddress() && (!lan || token == null || token.length() < 32)) {
            throw new IllegalArgumentException("Stream Deck LAN binding requires explicit opt-in and a token of at least 32 characters");
        }
        this.lan = !host.isLoopbackAddress();
        this.authorization = token == null || token.isBlank() ? null
                : ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
        this.runtime = runtime;
        this.catalog = new StreamDeckCatalog(directory);

        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.http.maxRequestSize = MAX_COMMAND_BYTES;
        });
        app.before(ctx -> {
            ctx.header("Cache-Control", "no-store");
            ctx.header("X-Content-Type-Options", "nosniff");
            if (ctx.header("Origin") != null || !this.lan && !InetAddress
                    .getByName(ctx.req().getRemoteAddr()).isLoopbackAddress()) {
                ctx.skipRemainingHandlers();
                error(ctx, 403, "network_forbidden");
            } else if (authorization != null && !authorized(ctx.header("Authorization"))) {
                ctx.skipRemainingHandlers();
                error(ctx, 401, "authentication_required");
            }
        });
        app.exception(Exception.class, (exception, ctx) -> {
            LOG.error("Stream Deck HTTP request failed", exception);
            error(ctx, 500, "internal_error");
        });
        app.exception(NotFoundResponse.class, (exception, ctx) -> error(ctx, 404, "not_found"));
        app.get(PREFIX + "/streamdeck-init", this::bootstrap);
        app.get(ROOT + "/health", this::health);
        app.get(ROOT + "/menus/{menuId}", this::menu);
        app.get(ROOT + "/assets/{name}", this::asset);
        app.get(ROOT + "/state", this::state);
        app.post(ROOT + "/actions", this::action);
        app.start(host.getHostAddress(), port);
    }

    public static StreamDeckServer start(StreamDeckRuntime runtime) throws IOException {
        return new StreamDeckServer(runtime, Path.of(System.getProperty("aopi.streamdeck.config", "config/streamdeck")),
                InetAddress.getByName(System.getProperty("aopi.streamdeck.host", "127.0.0.1")),
                Integer.getInteger("aopi.streamdeck.port", 7000), Boolean.getBoolean("aopi.streamdeck.lan"),
                System.getenv("AOPI_STREAMDECK_TOKEN"));
    }

    int port() {
        return app.port();
    }

    @Override
    public void close() {
        app.stop();
    }

    private boolean authorized(String header) {
        return header != null && MessageDigest.isEqual(authorization, header.getBytes(StandardCharsets.UTF_8));
    }

    private void bootstrap(Context ctx) {
        ctx.json(Map.of("protocol", StreamDeckProtocol.VERSION, "application", "AetherOnePi",
                "instanceId", instanceId, "revision", catalog.current().revision(), "pollIntervalMs", 1000,
                "endpoints", Map.of("menu", "./streamdeck/menus/{menuId}", "state", "./streamdeck/state",
                        "action", "./streamdeck/actions", "health", "./streamdeck/health"),
                "startMenu", "main", "capabilities", List.of("keys", "encoders", "touch", "state", "images"),
                "supportedDevices", List.of("plus-xl", "plus", "xl", "key-only"),
                "assetsBaseUrl", "./streamdeck/assets/"));
    }

    private void health(Context ctx) {
        ctx.json(Map.of("protocol", StreamDeckProtocol.VERSION, "applicationVersion", applicationVersion,
                "status", runtime.snapshot() == null ? "starting" : "ok", "instanceId", instanceId));
    }

    private void menu(Context ctx) {
        var configuration = catalog.current();
        String id = ctx.pathParam("menuId");
        String device = ctx.queryParam("device");
        if (device == null || !StreamDeckCatalog.DEVICES.containsKey(device)) {
            device = "key-only";
        }
        var menu = configuration.variants().getOrDefault(id + "." + device, configuration.menus().get(id));
        if (menu == null) {
            error(ctx, 404, "menu_not_found");
            return;
        }
        var capacity = StreamDeckCatalog.DEVICES.get(device);
        ctx.json(Map.of("id", menu.id(), "title", menu.title(), "revision", configuration.revision(),
                "keys", StreamDeckCatalog.compactKeys(menu, capacity.keys()), "encoders",
                java.util.Arrays.stream(menu.encoders()).filter(encoder -> encoder.slot() < capacity.encoders()).toList()));
    }

    private void asset(Context ctx) {
        String name = ctx.pathParam("name");
        var configuration = catalog.current();
        byte[] bytes = configuration.assets().get(name);
        if (bytes == null) {
            error(ctx, 404, "asset_not_found");
            return;
        }
        ctx.header("Content-Security-Policy", "default-src 'none'; sandbox");
        ctx.header("Cache-Control", "private, no-cache");
        String etag = "\"" + configuration.revision() + "\"";
        ctx.header("ETag", etag);
        if (etag.equals(ctx.header("If-None-Match"))) {
            ctx.status(304);
        } else {
            ctx.contentType(name.endsWith(".svg") ? "image/svg+xml" : "image/png").result(bytes);
        }
    }

    private void state(Context ctx) {
        var snapshot = runtime.snapshot();
        if (snapshot == null) {
            error(ctx, 503, "application_starting");
            return;
        }
        var body = new LinkedHashMap<String, Object>();
        body.put("instanceId", instanceId);
        body.put("menuRevision", catalog.current().revision());
        body.put("connected", true);
        body.put("commandsReady", runtime.animationReady());
        body.put("analysis", snapshot.analysis());
        body.put("gv", snapshot.gv());
        body.put("broadcast", snapshot.broadcast());
        body.put("rates", snapshot.rates());
        body.put("hotbits", snapshot.hotbits());
        body.put("jobs", runtime.commands().recentJobs());
        ctx.json(body);
    }

    private void action(Context ctx) throws IOException {
        if (runtime.snapshot() == null) {
            error(ctx, 503, "application_starting");
            return;
        }
        String contentType = ctx.contentType();
        if (contentType == null || !contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
            error(ctx, 400, "expected_json");
            return;
        }
        byte[] body = ctx.bodyInputStream().readNBytes(MAX_COMMAND_BYTES + 1);
        if (body.length > MAX_COMMAND_BYTES) {
            error(ctx, 413, "request_too_large");
            return;
        }
        Request request;
        try {
            request = StreamDeckCatalog.JSON.readValue(body, Request.class);
        } catch (IOException e) {
            error(ctx, 400, "malformed_request");
            return;
        }
        var result = runtime.commands().submit(request, runtime.animationReady());
        var job = result.job();
        var response = new LinkedHashMap<String, Object>();
        response.put("requestId", job.requestId());
        response.put("jobId", job.requestId());
        response.put("accepted", result.status() < 300);
        response.put("status", job.status());
        response.put("code", job.code());
        ctx.status(result.status()).json(response);
    }

    private static void error(Context ctx, int status, String code) {
        ctx.status(status).json(Map.of("code", code));
    }

    private static String readApplicationVersion() {
        String version = StreamDeckServer.class.getPackage().getImplementationVersion();
        if (version != null) {
            return version;
        }
        try (var input = StreamDeckServer.class.getResourceAsStream(
                "/META-INF/maven/de.isuret.polos/AetherOnePi/pom.properties")) {
            if (input != null) {
                Properties properties = new Properties();
                properties.load(input);
                return properties.getProperty("version", "development");
            }
        } catch (IOException e) {
            LOG.debug("Could not read AetherOnePi version metadata", e);
        }
        return "development";
    }
}
