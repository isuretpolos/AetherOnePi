import streamDeck, { action, SingletonAction, Target } from "@elgato/streamdeck";
import type { Action, DialDownEvent, DialRotateEvent, DialUpEvent, DidReceiveSettingsEvent, KeyDownEvent,
    TouchTapEvent, WillAppearEvent, WillDisappearEvent } from "@elgato/streamdeck";
import { defaultUrl, deviceInfo, slotIndex } from "./client.ts";
import type { Settings } from "./client.ts";
import { Surface } from "./surface.ts";

type SlotSettings = { slotIndex?: number };
let settings: Settings = { url: defaultUrl, token: "" };
const surface = new Surface((status) => send({ status }));

function send(payload: { status?: string; url?: string; hasToken?: boolean; pollInterval?: number }) {
    void streamDeck.ui.sendToPropertyInspector(payload).catch(() => undefined);
}

function register(control: Action<SlotSettings>, slotSettings: SlotSettings) {
    if (!control.isKey() && !control.isDial()) {
        return;
    }
    const { id, device } = control;
    const { rows, columns } = device.size;
    const capacity = deviceInfo(device.type, rows, columns);
    const kind = control.isKey() ? "key" : "encoder";
    const slot = slotIndex(capacity, kind, control.coordinates, slotSettings.slotIndex);
    surface.disappear(device.id, id);
    if (slot === undefined || capacity.keys < 6) {
        void control.showAlert().catch(() => undefined);
        return;
    }
    let previousImage: string | undefined;
    let previousTitle: string | undefined;
    let previousLayout: string | undefined;
    surface.appear({
        id, deviceId: device.id, device: capacity, kind, slot,
        alert: () => control.showAlert(),
        async draw(display, current) {
            if (control.isKey() && "icon" in display) {
                const image = display.disabled ? "imgs/dim.svg" : display.icon;
                if (image !== previousImage && current()) {
                    await control.setImage(image, { target: Target.HardwareAndSoftware });
                    previousImage = image;
                }
                if (display.title !== previousTitle && current()) {
                    await control.setTitle(display.title, { target: Target.HardwareAndSoftware });
                    previousTitle = display.title;
                }
            } else if (control.isDial() && "layout" in display) {
                if (display.layout !== previousLayout && current()) {
                    await control.setFeedbackLayout(`layouts/${display.layout}.json`);
                    previousLayout = display.layout;
                }
                if (current()) {
                    await control.setFeedback({
                        title: display.title,
                        value: display.value,
                        ...(display.layout === "progress-dial" ? { progress: display.progress ?? 0 } : {})
                    });
                }
            }
        }
    });
}

class SlotAction extends SingletonAction<SlotSettings> {
    override onWillAppear(event: WillAppearEvent<SlotSettings>) {
        register(event.action, event.payload.settings);
    }

    override onDidReceiveSettings(event: DidReceiveSettingsEvent<SlotSettings>) {
        register(event.action, event.payload.settings);
    }

    override onWillDisappear({ action: control }: WillDisappearEvent<SlotSettings>) {
        surface.disappear(control.device.id, control.id);
    }
}

@action({ UUID: "com.aetherone.streamdeck.key" })
class KeySlot extends SlotAction {
    override onKeyDown({ action: control }: KeyDownEvent<SlotSettings>) {
        surface.press(control.device.id, control.id, "key");
    }
}

@action({ UUID: "com.aetherone.streamdeck.encoder" })
class EncoderSlot extends SlotAction {
    private readonly pressed = new Set<string>();

    override onDialDown({ action: control }: DialDownEvent<SlotSettings>) {
        if (!this.pressed.has(control.id)) {
            this.pressed.add(control.id);
            surface.press(control.device.id, control.id, "push");
        }
    }

    override onDialUp({ action: control }: DialUpEvent<SlotSettings>) {
        this.pressed.delete(control.id);
    }

    override onDialRotate({ action: control, payload }: DialRotateEvent<SlotSettings>) {
        surface.rotate(control.device.id, control.id, payload.ticks);
    }

    override onTouchTap({ action: control, payload }: TouchTapEvent<SlotSettings>) {
        surface.press(control.device.id, control.id, payload.hold ? "longTouch" : "touch");
    }

    override onWillDisappear(event: WillDisappearEvent<SlotSettings>) {
        this.pressed.delete(event.action.id);
        super.onWillDisappear(event);
    }
}

function readSettings(value: Partial<Settings>): Settings {
    return {
        url: typeof value.url === "string" && value.url.trim() ? value.url.trim().slice(0, 1024) : defaultUrl,
        token: typeof value.token === "string" ? value.token.trim().slice(0, 512) : "",
        pollInterval: typeof value.pollInterval === "number" && Number.isSafeInteger(value.pollInterval)
            ? Math.max(500, Math.min(10000, value.pollInterval)) : undefined
    };
}

function report() {
    send({ status: surface.connectionStatus, url: settings.url,
        hasToken: Boolean(settings.token), pollInterval: settings.pollInterval });
}

async function inspector(payload: unknown, setSlot: (value: SlotSettings) => Promise<void>) {
    if (typeof payload !== "object" || payload === null) {
        return;
    }
    const message = payload as { type?: string; url?: string; token?: string; clearToken?: boolean;
        pollInterval?: number; slotIndex?: number };
    if (message.type === "status") {
        report();
    } else if (message.type === "slot") {
        if (message.slotIndex === undefined || Number.isInteger(message.slotIndex) && message.slotIndex >= 0
                && message.slotIndex < 36) {
            await setSlot(message.slotIndex === undefined ? {} : { slotIndex: message.slotIndex });
        }
    } else if (message.type === "connect") {
        const next = readSettings({
            url: message.url, token: message.clearToken ? "" : message.token || settings.token,
            pollInterval: message.pollInterval
        });
        await streamDeck.settings.setGlobalSettings(next);
        settings = next;
        surface.configure(settings);
        report();
    } else if (message.type === "home") {
        surface.configure(settings, true);
    }
}

streamDeck.actions.registerAction(new KeySlot());
streamDeck.actions.registerAction(new EncoderSlot());
streamDeck.ui.onSendToPlugin(({ payload, action: control }) => {
    void inspector(payload, async (value) => {
        await control.setSettings(value);
        // Programmatic writes do not emit a settings-changed event in SDK 3.
        register(control, value);
    }).catch(() => send({ status: "Could not save Stream Deck settings." }));
});

void streamDeck.connect().then(async () => {
    settings = readSettings(await streamDeck.settings.getGlobalSettings<Settings>());
    surface.configure(settings);
    report();
}).catch(() => {
    streamDeck.logger.error("Could not initialize AetherOnePi Stream Deck settings.");
});
