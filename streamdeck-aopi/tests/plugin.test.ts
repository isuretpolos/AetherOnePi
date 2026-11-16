import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { test } from "node:test";
import { Client, defaultUrl, deviceInfo, safeImage, slotIndex, trustedLink } from "../src/client.ts";
import { parseMenu } from "../src/protocol.ts";
import { Surface } from "../src/surface.ts";
import type { Control, Display } from "../src/surface.ts";

const bootstrap = {
    protocol: "aopi-streamdeck/1", application: "AetherOnePi", instanceId: "instance-1",
    revision: "one", pollIntervalMs: 1000, startMenu: "main",
    endpoints: {
        menu: "./streamdeck/menus/{menuId}", state: "./streamdeck/state",
        action: "./streamdeck/actions", health: "./streamdeck/health"
    },
    assetsBaseUrl: "./streamdeck/assets/"
};

test("maps all 36 + XL keys and all six encoder coordinates exactly once", () => {
    const device = deviceInfo(13, 4, 9);
    const keys = Array.from({ length: 36 }, (_, slot) =>
        slotIndex(device, "key", { row: Math.floor(slot / 9), column: slot % 9 }));
    const encoders = Array.from({ length: 6 }, (_, slot) =>
        slotIndex(device, "encoder", { row: 0, column: slot }));
    assert.deepEqual(keys, Array.from({ length: 36 }, (_, slot) => slot));
    assert.deepEqual(encoders, [0, 1, 2, 3, 4, 5]);
    assert.equal(slotIndex(device, "key", { row: 0, column: 9 }), undefined);
    assert.equal(slotIndex(device, "encoder", { row: 1, column: 1 }), undefined);
    assert.equal(slotIndex(device, "key", { row: 1, column: 1 }, 35), 35);
    assert.equal(slotIndex(deviceInfo(2, 4, 8), "encoder", { row: 0, column: 0 }), undefined);
});

test("rejects off-origin links, unsafe SVGs and invalid menu slots", () => {
    const url = new URL(defaultUrl);
    assert.throws(() => trustedLink("https://example.org/capture", url, "./streamdeck/state"));
    assert.throws(() => trustedLink("./other", url, "./streamdeck/state"));
    assert.equal(trustedLink("./streamdeck/state", url, "./streamdeck/state").pathname, "/aopi/api/streamdeck/state");
    assert.throws(() => safeImage(Buffer.from('<svg><script>alert(1)</script></svg>'), "image/svg+xml", "bad.svg"));
    const key = { slot: 0, title: "NEXT", enabled: true, press: { type: "navigate", menuId: "broadcast" } };
    assert.throws(() => parseMenu({ id: "main", revision: "one", keys: [key, key], encoders: [] }, "main", 6, 0));
    assert.throws(() => parseMenu({ id: "main", revision: "one", keys: [{ ...key, icon: "../x.svg" }], encoders: [] },
        "main", 6, 0));
});

test("rejects incompatible bootstrap and does not follow redirects", async (t) => {
    const fetch = t.mock.method(globalThis, "fetch", async (_url: URL, init: RequestInit) => {
        assert.equal(init.redirect, "error");
        return Response.json({ ...bootstrap, protocol: "aopi-streamdeck/2" });
    });
    await assert.rejects(Client.connect({ url: defaultUrl, token: "" }), /Unsupported/);
    assert.equal(fetch.mock.callCount(), 1);
});

test("navigates independent devices, batches rotation, renders revisions and recovers after offline", async (t) => {
    const menuSource = async (id: string) => JSON.parse(await readFile(
        new URL(`../../src/main/resources/streamdeck/menus/${id}.json`, import.meta.url), "utf8"));
    const main = await menuSource("main");
    const analysis = await menuSource("analysis");
    const broadcast = await menuSource("broadcast");
    const icon = await readFile(new URL("../../src/main/resources/streamdeck/assets/broadcast.svg", import.meta.url), "utf8");
    const sent: { command: string; args: object; source: { deviceId: string; slot: number } }[] = [];
    let revision = "one";
    let duration = 60;
    let offline = false;
    const fetched = t.mock.method(globalThis, "fetch", async (url: URL, init: RequestInit) => {
        const path = url.pathname;
        assert.equal(new Headers(init.headers).get("Authorization"), "Bearer local-token");
        if (offline) {
            throw new Error("Disconnected");
        }
        if (path.endsWith("streamdeck-init")) {
            return Response.json({ ...bootstrap, revision });
        }
        if (path.endsWith("/state")) {
            return Response.json({
                connected: true, instanceId: "instance-1", menuRevision: revision, jobs: [],
                analysis: { running: false, lastResultCount: 2 }, gv: { value: 500 },
                broadcast: { durationSeconds: duration, status: "IDLE", canStart: true },
                rates: { selectedLabel: "Example", candidateLabel: "Example" }
            });
        }
        if (path.endsWith("/main") || path.endsWith("/broadcast") || path.endsWith("/analysis")) {
            const menu = path.endsWith("/main") ? main : path.endsWith("/analysis") ? analysis : broadcast;
            return Response.json({ ...menu, revision,
                encoders: url.searchParams.get("device") === "key-only" ? [] : menu.encoders,
                keys: revision === "one" ? menu.keys : menu.keys.filter((key: { slot: number }) => key.slot !== 0)
            });
        }
        if (path.endsWith("/actions")) {
            const request = JSON.parse(String(init.body));
            sent.push(request);
            if (request.command === "broadcast.adjustDuration") {
                duration += 10 * request.args.deltaSteps;
            } else if (request.command === "broadcast.resetDuration") {
                duration = 60;
            }
            return Response.json({ requestId: request.requestId, accepted: true, status: "completed" });
        }
        return new Response(icon, { headers: { "Content-Type": "image/svg+xml" } });
    });

    const displays = new Map<string, Display>();
    const surface = new Surface(() => undefined);
    t.after(() => surface.close());
    const device = deviceInfo(13, 4, 9);
    const add = (deviceId: string, kind: Control["kind"], slot: number) => {
        const id = `${deviceId}-${kind}-${slot}`;
        surface.appear({ id, deviceId, device, kind, slot,
            async draw(display) { displays.set(id, display); }, async alert() {} });
        return id;
    };
    const first = add("first", "key", 0);
    const second = add("second", "key", 0);
    const secondBack = add("second", "key", 4);
    const navigate = add("first", "key", 1);
    const back = add("first", "key", 4);
    const dial = add("first", "encoder", 0);
    const touch = add("second", "encoder", 1);

    surface.configure({ url: defaultUrl, token: "local-token" });
    await until(() => displays.get(first)?.title === "ANALYSIS" && displays.get(second)?.title === "ANALYSIS");
    surface.press("second", second, "key");
    await until(() => sent[0]?.command === "analysis.show" && displays.get(second)?.title === "START");
    assert.equal(displays.get(first)?.title, "ANALYSIS");
    sent.shift();
    surface.press("second", secondBack, "key");
    await until(() => displays.get(second)?.title === "ANALYSIS");
    surface.press("first", navigate, "key");
    await until(() => displays.get(first)?.title === "START");
    assert.equal(displays.get(second)?.title, "ANALYSIS");
    const rotatedAt = Date.now();
    surface.rotate("first", dial, 1);
    surface.rotate("first", dial, 2);
    await until(() => sent.length === 1);
    assert.deepEqual(sent[0].args, { deltaSteps: 3 });
    assert.equal(sent[0].source.deviceId, "first");
    await until(() => {
        const display = displays.get(dial);
        return display !== undefined && "value" in display && display.value === "90s";
    });
    assert.ok(Date.now() - rotatedAt < 700, "Dial feedback should arrive before the normal state poll");
    surface.press("first", dial, "push");
    await until(() => sent[1]?.command === "broadcast.resetDuration");
    surface.press("second", touch, "touch");
    await until(() => displays.get(second)?.title === "START");
    surface.press("first", back, "key");
    await until(() => displays.get(first)?.title === "ANALYSIS");
    assert.equal(displays.get(second)?.title, "START");

    const countImages = () => fetched.mock.calls.filter((call) => String(call.arguments[0]).includes("/assets/")).length;
    const images = countImages();
    await new Promise((resolve) => setTimeout(resolve, 80));
    assert.equal(countImages(), images);

    revision = "two";
    await until(() => displays.get(first)?.title === "" && displays.get(second)?.title === "");
    assert.ok(countImages() > images);
    offline = true;
    await until(() => displays.get(first)?.title === "OFFLINE");
    surface.press("first", navigate, "key");
    assert.equal(sent.length, 2);
    offline = false;
    await until(() => displays.get(second)?.title === "");
    assert.equal(displays.get(navigate)?.title, "BROADCAST");
});

async function until(predicate: () => boolean) {
    const deadline = Date.now() + 3000;
    while (!predicate()) {
        assert.ok(Date.now() < deadline, "Timed out waiting for display or command");
        await new Promise((resolve) => setTimeout(resolve, 10));
    }
}
