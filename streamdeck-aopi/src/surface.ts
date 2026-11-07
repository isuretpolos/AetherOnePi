import { randomUUID } from "node:crypto";
import { Client } from "./client.ts";
import type { Device, Settings } from "./client.ts";
import type { Command, Encoder, Interaction, Key, Menu, MenuId, State } from "./protocol.ts";

type KeyDisplay = { title: string; icon?: string; disabled: boolean };
type DialDisplay = { title: string; value: string; layout: Encoder["layout"]; progress?: number };
export type Display = KeyDisplay | DialDisplay;

export type Control = {
    id: string;
    deviceId: string;
    device: Device;
    kind: "key" | "encoder";
    slot: number;
    draw(display: Display, current: () => boolean): Promise<void>;
    alert(): Promise<void>;
};

type VisibleControl = Control & {
    display?: Display;
    painting: boolean;
    repaint?: boolean;
    lastPress?: number;
    rotation?: { menu: Menu; ticks: number; timer: NodeJS.Timeout };
};

type Session = {
    controls: Map<string, VisibleControl>;
    stack: MenuId[];
    menu?: Menu;
    navigating: boolean;
};

const blank = "imgs/blank.svg";

export class Surface {
    private readonly devices = new Map<string, Session>();
    private readonly jobs = new Map<string, { control: VisibleControl; expires: number }>();
    private client?: Client;
    private state?: State;
    private instanceId?: string;
    private settings?: Settings;
    private timer?: NodeJS.Timeout;
    private fastPollUntil = 0;
    private generation = 0;
    private failures = 0;
    private status = "Enter the AetherOnePi bootstrap URL.";

    constructor(private readonly onStatus: (status: string) => void) {}

    configure(settings: Settings, reset = false) {
        const changed = settings.url !== this.settings?.url || settings.token !== this.settings?.token;
        this.settings = settings;
        ++this.generation;
        clearTimeout(this.timer);
        this.timer = undefined;
        this.fastPollUntil = 0;
        this.client = undefined;
        this.state = undefined;
        this.failures = 0;
        if (changed || reset) {
            this.resetMenus();
        }
        this.setStatus("Connecting…");
        this.renderAll();
        void this.poll(this.generation);
    }

    get connectionStatus() {
        return this.status;
    }

    close() {
        ++this.generation;
        clearTimeout(this.timer);
        for (const session of this.devices.values()) {
            for (const control of session.controls.values()) {
                clearTimeout(control.rotation?.timer);
            }
        }
        this.devices.clear();
        this.jobs.clear();
    }

    appear(control: Control) {
        let session = this.devices.get(control.deviceId);
        if (!session) {
            session = { controls: new Map(), stack: [], navigating: false };
            this.devices.set(control.deviceId, session);
        }
        const duplicate = [...session.controls.values()].find((item) =>
            item.kind === control.kind && item.slot === control.slot && item.id !== control.id);
        if (duplicate) {
            this.alert(control);
            return;
        }
        const previous = session.controls.get(control.id);
        if (previous) {
            this.disappear(control.deviceId, control.id);
        }
        const visible = { ...control, painting: false };
        session.controls.set(control.id, visible);
        this.render(session);
    }

    disappear(deviceId: string, id: string) {
        const controls = this.devices.get(deviceId)?.controls;
        const control = controls?.get(id);
        if (control) {
            clearTimeout(control.rotation?.timer);
            controls?.delete(id);
        }
    }

    press(deviceId: string, id: string, input: "key" | "push" | "touch" | "longTouch") {
        const session = this.devices.get(deviceId);
        const control = session?.controls.get(id);
        if (!session || !control || !session.menu || !this.client || !this.state) {
            return;
        }
        const key = input === "key" ? session.menu.keys.find((item) => item.slot === control.slot) : undefined;
        const dial = session.menu.encoders.find((item) => item.slot === control.slot);
        const action = input === "key" ? key?.press : dial?.[input];
        const now = Date.now();
        if (!action || key && !this.keyEnabled(key) || now - (control.lastPress ?? 0) < 200) {
            return;
        }
        control.lastPress = now;
        if (action.type === "command") {
            void this.command(control, session.menu, action.command);
        } else {
            void this.navigate(session, control, action);
        }
    }

    rotate(deviceId: string, id: string, ticks: number) {
        const session = this.devices.get(deviceId);
        const control = session?.controls.get(id);
        const menu = session?.menu;
        const command = menu?.encoders.find((item) => item.slot === control?.slot)?.rotateCommand;
        if (!control || !menu || !command || !this.state?.commandsReady || !Number.isSafeInteger(ticks)) {
            return;
        }
        if (control.rotation?.menu === menu) {
            control.rotation.ticks = Math.max(-20, Math.min(20, control.rotation.ticks + ticks));
            return;
        }
        clearTimeout(control.rotation?.timer);
        const generation = this.generation;
        control.rotation = {
            menu, ticks: Math.max(-20, Math.min(20, ticks)),
            timer: setTimeout(() => {
                const rotation = control.rotation;
                control.rotation = undefined;
                if (rotation?.ticks && session.menu === menu && generation === this.generation) {
                    void this.command(control, menu, command, rotation.ticks);
                }
            }, 110)
        };
    }

    private async navigate(session: Session, control: VisibleControl, action: Interaction) {
        const client = this.client;
        const previous = session.menu;
        if (!client || !previous || session.navigating) {
            return;
        }
        const start = client.bootstrap.startMenu;
        const target = action.type === "navigate" ? action.menuId : action.type === "home" ? start
            : action.type === "back" ? session.stack.at(-1) ?? start : undefined;
        if (!target || target === previous.id) {
            return;
        }
        session.navigating = true;
        try {
            const menu = await client.menu(target, control.device);
            if (this.client !== client || session.menu !== previous || menu.revision !== this.state?.menuRevision) {
                return;
            }
            if (action.type === "navigate") {
                session.stack.push(previous.id);
                if (session.stack.length > 32) {
                    session.stack.shift();
                }
            } else if (action.type === "back") {
                session.stack.pop();
            } else {
                session.stack = [];
            }
            session.menu = menu;
            this.render(session);
        } catch {
            this.alert(control);
        } finally {
            session.navigating = false;
        }
    }

    private async command(control: VisibleControl, menu: Menu, command: Command, ticks?: number) {
        const client = this.client;
        if (!client || !this.state || menu.revision !== this.state.menuRevision
                || !this.state.commandsReady && command !== "broadcast.stop") {
            return;
        }
        const requestId = randomUUID();
        try {
            const queued = await client.action(requestId, command, {
                deviceId: control.deviceId, menuId: menu.id, slot: control.slot
            }, ticks);
            if (this.client === client) {
                if (queued && this.jobs.size < 128) {
                    this.jobs.set(requestId, { control, expires: Date.now() + 60000 });
                }
                this.fastPollUntil = Date.now() + 1000;
                if (this.timer) {
                    clearTimeout(this.timer);
                    this.schedulePoll(this.generation, 45);
                }
            }
        } catch {
            this.alert(control);
        }
    }

    private async poll(generation: number) {
        if (generation !== this.generation) {
            return;
        }
        this.timer = undefined;
        const settings = this.settings;
        if (!settings) {
            return;
        }
        let delay: number;
        try {
            const client = this.client ?? await Client.connect(settings);
            const state = await client.state();
            if (generation !== this.generation) {
                return;
            }
            if (client.bootstrap.instanceId !== state.instanceId) {
                throw new Error("AetherOnePi restarted");
            }
            if (this.instanceId && this.instanceId !== state.instanceId) {
                this.resetMenus();
            }
            this.instanceId = state.instanceId;
            this.client = client;
            this.state = state;
            this.failures = 0;
            this.setStatus(state.commandsReady ? `Connected to ${client.bootstrap.application} · aopi-streamdeck/1`
                : "AetherOnePi is busy · STOP is available");
            this.finishJobs(state);
            await Promise.all([...this.devices.values()].map((session) => this.refresh(session, client, state)));
            delay = settings.pollInterval ?? client.bootstrap.pollIntervalMs;
            if (Date.now() < this.fastPollUntil && this.jobs.size > 0) {
                delay = Math.min(delay, 100);
            }
        } catch (error) {
            if (generation !== this.generation) {
                return;
            }
            this.client = undefined;
            this.state = undefined;
            this.setStatus(`Offline · ${error instanceof Error ? error.message : "Connection failed"}`);
            this.renderAll();
            delay = Math.min(30000, 1000 * 2 ** Math.min(this.failures++, 5)) + Math.random() * 250;
        }
        if (generation === this.generation) {
            this.schedulePoll(generation, delay);
        }
    }

    private schedulePoll(generation: number, delay: number) {
        this.timer = setTimeout(() => void this.poll(generation), delay);
    }

    private async refresh(session: Session, client: Client, state: State) {
        const control = session.controls.values().next().value;
        if (!control || session.navigating) {
            return;
        }
        if (session.menu?.revision !== state.menuRevision) {
            const current = session.menu;
            const start = client.bootstrap.startMenu;
            try {
                const menu = await client.menu(current?.id ?? start, control.device);
                if (this.client === client && this.state === state && session.menu === current
                        && !session.navigating && menu.revision === state.menuRevision) {
                    session.menu = menu;
                }
            } catch {
                // Keep the last menu visible, with actions gated by its revision.
            }
        }
        if (this.client === client) {
            this.render(session);
        }
    }

    private finishJobs(state: State) {
        for (const [id, job] of this.jobs) {
            const result = state.jobs.get(id);
            if (result === "rejected") {
                this.alert(job.control);
            }
            if (result === "completed" || result === "rejected" || job.expires < Date.now()) {
                this.jobs.delete(id);
            }
        }
    }

    private resetMenus() {
        this.instanceId = undefined;
        this.jobs.clear();
        for (const session of this.devices.values()) {
            session.menu = undefined;
            session.stack = [];
        }
    }

    private keyEnabled(key: Key) {
        if (!key.enabled) {
            return false;
        }
        const command = key.press?.type === "command" ? key.press.command : undefined;
        if (command && command !== "broadcast.stop" && !this.state?.commandsReady) {
            return false;
        }
        return command !== "broadcast.start" || this.state?.canBroadcast === true;
    }

    private setStatus(status: string) {
        if (this.status !== status) {
            this.status = status;
            this.onStatus(status);
        }
    }

    private renderAll() {
        for (const session of this.devices.values()) {
            this.render(session);
        }
    }

    private render(session: Session) {
        for (const control of session.controls.values()) {
            const current = () => session.controls.get(control.id) === control;
            if (control.painting) {
                control.repaint = true;
            } else {
                void this.paintLatest(session, control, current);
            }
        }
    }

    private async paintLatest(session: Session, control: VisibleControl, current: () => boolean) {
        control.painting = true;
        do {
            control.repaint = false;
            await this.paint(session, control, current);
        } while (control.repaint && current());
        control.painting = false;
    }

    private async paint(session: Session, control: VisibleControl, current: () => boolean) {
        const menu = session.menu;
        const client = this.client;
        const state = this.state;
        const online = menu?.revision === state?.menuRevision && state !== undefined;
        const stillCurrent = () => current() && session.menu === menu && this.client === client;
        const value = (key?: keyof State["values"]) => key ? state?.values[key] : undefined;
        try {
            let display: Display;
            if (control.kind === "key") {
                const key = menu?.keys.find((item) => item.slot === control.slot);
                let icon: string | undefined;
                if (online && key?.icon && menu && client) {
                    icon = await client.icon(key.icon, menu.revision).catch(() => undefined);
                }
                const text = value(key?.valueKey);
                const running = state?.analyzing && key?.press?.type === "command"
                    && key.press.command === "analysis.start";
                let title = "";
                if (!online && control.slot === 0) {
                    title = state ? "MENU ERROR" : "OFFLINE";
                } else if (online && key) {
                    title = running ? "RUNNING" : text == null ? key.title : `${key.title}\n${String(text).slice(0, 22)}`;
                }
                display = { title, icon: icon ?? blank, disabled: !!key?.press && !this.keyEnabled(key) };
            } else {
                const dial = menu?.encoders.find((item) => item.slot === control.slot);
                const text = online ? value(dial?.valueKey) : undefined;
                display = {
                    title: online ? dial?.title ?? "" : control.slot === 0 ? state ? "MENU ERROR" : "OFFLINE" : "",
                    value: text == null ? dial && online ? "—" : "" : `${String(text).slice(0, 30)}${dial?.unit ?? ""}`,
                    layout: dial?.layout ?? "status-dial",
                    progress: dial?.layout === "progress-dial" && typeof text === "number" ? text : undefined
                };
            }
            if (stillCurrent() && JSON.stringify(control.display) !== JSON.stringify(display)) {
                await control.draw(display, stillCurrent);
                if (stillCurrent()) {
                    control.display = display;
                }
            }
        } catch {
            // The action may have disappeared while the SDK call was in flight.
        }
    }

    private alert(control: Control) {
        void control.alert().catch(() => undefined);
    }
}
