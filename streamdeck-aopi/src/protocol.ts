export const protocol = "aopi-streamdeck/1";
export const menuIds = ["main", "analysis", "broadcast", "rates", "settings"] as const;
export const layouts = ["value-dial", "status-dial", "progress-dial", "navigation-dial"] as const;
const commands = ["analysis.show", "analysis.start", "analysis.clear", "gv.measure", "broadcast.start", "broadcast.stop",
    "broadcast.adjustDuration", "broadcast.resetDuration", "rates.next", "rates.previous", "rates.select"] as const;
export const valueKeys = ["analysis.lastResultCount", "gv.value", "broadcast.durationSeconds",
    "broadcast.progressPercent", "broadcast.status", "rates.selectedLabel", "rates.candidateLabel", "hotbits.status"] as const;

export type MenuId = typeof menuIds[number];
export type Command = typeof commands[number];
export type ValueKey = typeof valueKeys[number];
export type Layout = typeof layouts[number];
export type DeviceClass = "plus-xl" | "plus" | "xl" | "key-only";
export type Interaction = { type: "navigate"; menuId: MenuId; command?: "analysis.show" }
    | { type: "command"; command: Command }
    | { type: "home" | "back" | "none" };

export type Key = {
    slot: number;
    title: string;
    icon?: string;
    enabled: boolean;
    valueKey?: ValueKey;
    press?: Interaction;
};
export type Encoder = {
    slot: number;
    title: string;
    layout: Layout;
    valueKey?: ValueKey;
    unit?: string;
    rotateCommand?: Command;
    push?: Interaction;
    touch?: Interaction;
    longTouch?: Interaction;
};
export type Menu = { id: MenuId; revision: string; keys: Key[]; encoders: Encoder[] };
export type State = {
    instanceId: string;
    menuRevision: string;
    values: Partial<Record<ValueKey, string | number>>;
    canBroadcast: boolean;
    commandsReady: boolean;
    analyzing: boolean;
    jobs: Map<string, string>;
};

export const assetName = /^[a-zA-Z0-9_-]{1,48}\.(svg|png)$/;
const revisionPattern = /^[A-Za-z0-9_-]{1,80}$/;

export function object(value: unknown): Record<string, unknown> {
    if (typeof value !== "object" || value === null || Array.isArray(value)) {
        throw new Error("Expected a JSON object");
    }
    return value as Record<string, unknown>;
}

export function label(value: unknown, max = 80): string {
    if (typeof value !== "string" || !value.trim() || value.length > max || /[\x00-\x1f\x7f]/.test(value)) {
        throw new Error("Invalid text");
    }
    return value;
}

function revision(value: unknown): string {
    const text = label(value);
    if (!revisionPattern.test(text)) {
        throw new Error("Invalid revision");
    }
    return text;
}

function member<T extends string>(value: unknown, allowed: readonly T[]): T {
    if (!allowed.includes(value as T)) {
        throw new Error("Unexpected protocol value");
    }
    return value as T;
}

function optional<T>(value: unknown, parse: (value: unknown) => T): T | undefined {
    return value == null ? undefined : parse(value);
}

function interaction(value: unknown): Interaction {
    const item = object(value);
    switch (item.type) {
        case "navigate": return {
            type: item.type,
            menuId: member(item.menuId, menuIds),
            command: optional(item.command, (command) => member(command, ["analysis.show"] as const))
        };
        case "command": return { type: item.type, command: member(item.command, commands) };
        case "home":
        case "back":
        case "none": return { type: item.type };
        default: throw new Error("Unknown action type");
    }
}

function slots<T>(value: unknown, capacity: number, parse: (item: Record<string, unknown>, slot: number) => T): T[] {
    if (!Array.isArray(value) || value.length > capacity) {
        throw new Error("Invalid number of slots");
    }
    const seen = new Set<number>();
    return value.map((item) => {
        const data = object(item);
        const slot = data.slot;
        if (typeof slot !== "number" || !Number.isInteger(slot) || slot < 0 || slot >= capacity || seen.has(slot)) {
            throw new Error("Invalid or duplicate slot");
        }
        seen.add(slot);
        return parse(data, slot);
    });
}

export function parseMenu(value: unknown, id: MenuId, keyCount: number, encoderCount: number): Menu {
    const data = object(value);
    if (data.id !== id) {
        throw new Error("Unexpected menu");
    }
    const keys = slots(data.keys, keyCount, (item, slot) => {
        if (typeof item.enabled !== "boolean") {
            throw new Error("Invalid enabled flag");
        }
        return {
            slot, title: label(item.title, 40), enabled: item.enabled,
            icon: optional(item.icon, (icon) => {
                if (typeof icon !== "string" || !assetName.test(icon)) {
                    throw new Error("Invalid icon reference");
                }
                return icon;
            }),
            valueKey: optional(item.valueKey, (key) => member(key, valueKeys)),
            press: optional(item.press, interaction)
        };
    });
    if (id !== "main" && !keys.some((key) => key.enabled && (key.press?.type === "back" || key.press?.type === "home"))) {
        throw new Error("Menu lacks an exit");
    }
    if (id === "broadcast" && !keys.some((key) => key.enabled && key.press?.type === "command"
            && key.press.command === "broadcast.stop")) {
        throw new Error("Broadcast menu lacks STOP");
    }
    const encoders = slots(data.encoders, encoderCount, (item, slot) => ({
        slot, title: label(item.title, 40), layout: member(item.layout, layouts),
        valueKey: optional(item.valueKey, (key) => member(key, valueKeys)),
        unit: optional(item.unit, (unit) => label(unit, 8)),
        rotateCommand: optional(item.rotateCommand,
            (command) => member(command, ["rates.next", "broadcast.adjustDuration"] as const)),
        push: optional(item.push, interaction),
        touch: optional(item.touch, interaction),
        longTouch: optional(item.longTouch, interaction)
    }));
    return { id, revision: revision(data.revision), keys, encoders };
}

export function parseState(value: unknown): State {
    const data = object(value);
    if (data.connected !== true) {
        throw new Error("Application is unavailable");
    }
    const values: State["values"] = {};
    for (const key of valueKeys) {
        const [section, property] = key.split(".");
        if (section === "hotbits" && data[section] == null) {
            continue;
        }
        const entry = object(data[section])[property];
        if (typeof entry === "string") {
            values[key] = entry.replace(/[\x00-\x1f\x7f]/g, " ").slice(0, 120);
        } else if (typeof entry === "number" && Number.isFinite(entry)) {
            values[key] = entry;
        } else if (entry != null) {
            throw new Error("Invalid application state");
        }
    }
    const jobs = new Map<string, string>();
    if (!Array.isArray(data.jobs) || data.jobs.length > 16) {
        throw new Error("Invalid command status");
    }
    for (const entry of data.jobs) {
        const job = object(entry);
        jobs.set(label(job.requestId, 36), member(job.status, ["queued", "completed", "rejected"]));
    }
    return {
        instanceId: revision(data.instanceId),
        menuRevision: revision(data.menuRevision),
        values,
        canBroadcast: object(data.broadcast).canStart === true,
        commandsReady: data.commandsReady !== false,
        analyzing: object(data.analysis).running === true,
        jobs
    };
}

export function parseBootstrap(value: unknown) {
    const data = object(value);
    if (data.protocol !== protocol) {
        throw new Error("Unsupported AetherOnePi Stream Deck protocol");
    }
    if (typeof data.pollIntervalMs !== "number" || !Number.isSafeInteger(data.pollIntervalMs)) {
        throw new Error("Invalid poll interval");
    }
    return {
        instanceId: revision(data.instanceId),
        revision: revision(data.revision),
        application: label(data.application),
        pollIntervalMs: Math.max(500, Math.min(10000, data.pollIntervalMs)),
        startMenu: member(data.startMenu, menuIds),
        links: object(data.endpoints),
        assets: data.assetsBaseUrl
    };
}
