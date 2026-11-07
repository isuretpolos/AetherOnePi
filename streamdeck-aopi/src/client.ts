import { assetName, object, parseBootstrap, parseMenu, parseState } from "./protocol.ts";
import type { Command, DeviceClass, MenuId } from "./protocol.ts";

const attribute = ' (?:d|cx|cy|r|x|y|width|height|rx|fill|stroke|stroke-width|stroke-linecap|stroke-linejoin)="[a-zA-Z0-9 .,#+-]{1,4096}"';
const svg = new RegExp('^<svg xmlns="http://www.w3.org/2000/svg" viewBox="[0-9 .-]{1,40}">\\s*'
    + `(?:<(?:path|rect|circle)(?:${attribute}){1,12}/>[ \\r\\n\\t]*){1,32}</svg>\\s*$`);
const pngHeader = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82]);

export const defaultUrl = "http://127.0.0.1:7000/aopi/api/streamdeck-init";

export type Settings = { url: string; token: string; pollInterval?: number };
export type Device = { deviceClass: DeviceClass; keys: number; encoders: number; columns: number };

export function deviceInfo(type: number, rows: number, columns: number): Device {
    const deviceClass = type === 13 ? "plus-xl" : type === 7 ? "plus" : type === 2 ? "xl" : "key-only";
    return { deviceClass, keys: rows * columns, columns, encoders: type === 13 ? 6 : type === 7 ? 4 : 0 };
}

export function slotIndex(device: Device, controller: "key" | "encoder",
                          coordinates?: { row: number; column: number }, explicit?: number): number | undefined {
    let index = explicit;
    if (index === undefined && coordinates) {
        const { row, column } = coordinates;
        if (Number.isInteger(row) && row >= 0 && Number.isInteger(column) && column >= 0
                && (controller === "key" ? column < device.columns : row === 0)) {
            index = controller === "key" ? row * device.columns + column : column;
        }
    }
    const limit = controller === "key" ? device.keys : device.encoders;
    return index !== undefined && Number.isInteger(index) && index >= 0 && index < limit ? index : undefined;
}

function safeUrl(value: string): URL {
    const url = new URL(value);
    if (!["http:", "https:"].includes(url.protocol) || url.username || url.password || url.hash || url.search
            || !url.pathname.endsWith("/streamdeck-init")) {
        throw new Error("Enter an HTTP(S) Stream Deck bootstrap URL without credentials or a query");
    }
    return url;
}

export function trustedLink(value: unknown, bootstrap: URL, expected: string): URL {
    if (typeof value !== "string" || value.length > 1024) {
        throw new Error("Invalid bootstrap link");
    }
    const resolved = new URL(value, bootstrap);
    const allowed = new URL(expected, bootstrap);
    if (resolved.href !== allowed.href || resolved.username || resolved.password) {
        throw new Error("Bootstrap link points outside the AetherOnePi API");
    }
    return allowed;
}

export function safeImage(bytes: Buffer, mime: string, filename: string): string {
    if (filename.endsWith(".svg") && mime === "image/svg+xml") {
        const text = bytes.toString("utf8");
        if (svg.test(text)) {
            const image = text.replace("<svg ", '<svg width="112" height="112" ');
            return `data:image/svg+xml,${encodeURIComponent(image)}`;
        }
    }
    if (filename.endsWith(".png") && mime === "image/png" && bytes.length >= 33
            && bytes.subarray(0, 16).equals(pngHeader)
            && [bytes.readUInt32BE(16), bytes.readUInt32BE(20)].every((size) => size >= 1 && size <= 512)) {
        return `data:image/png;base64,${bytes.toString("base64")}`;
    }
    throw new Error("Unsupported or unsafe icon");
}

class HttpError extends Error {
    constructor(readonly status: number) {
        super(`AetherOnePi returned HTTP ${status}`);
    }
}

export class Client {
    readonly bootstrap;
    private readonly icons = new Map<string, Promise<string>>();
    private imageRevision?: string;

    private constructor(private readonly url: URL, private readonly settings: Settings,
                        bootstrap: ReturnType<typeof parseBootstrap>) {
        this.bootstrap = bootstrap;
    }

    static async connect(settings: Settings): Promise<Client> {
        const url = safeUrl(settings.url);
        const response = await request(url, settings.token);
        const bootstrap = parseBootstrap(await json(response));
        trustedLink(bootstrap.links.menu, url, "./streamdeck/menus/{menuId}");
        trustedLink(bootstrap.links.state, url, "./streamdeck/state");
        trustedLink(bootstrap.links.action, url, "./streamdeck/actions");
        trustedLink(bootstrap.links.health, url, "./streamdeck/health");
        trustedLink(bootstrap.assets, url, "./streamdeck/assets/");
        return new Client(url, settings, bootstrap);
    }

    async state() {
        return parseState(await json(await request(new URL("./streamdeck/state", this.url), this.settings.token)));
    }

    async menu(id: MenuId, device: Device) {
        const url = new URL(`./streamdeck/menus/${id}`, this.url);
        url.searchParams.set("device", device.deviceClass);
        const response = await request(url, this.settings.token);
        return parseMenu(await json(response), id, device.keys, device.encoders);
    }

    async action(requestId: string, command: Command, source: { deviceId: string; menuId: MenuId; slot: number },
                 deltaSteps?: number) {
        const response = await request(new URL("./streamdeck/actions", this.url), this.settings.token, {
            requestId, command, source, args: deltaSteps === undefined ? {} : { deltaSteps }
        });
        const job = object(await json(response));
        if (job.requestId !== requestId || job.accepted !== true || !["queued", "completed"].includes(String(job.status))) {
            throw new Error("Command was rejected");
        }
        return job.status === "queued";
    }

    icon(filename: string, revision: string): Promise<string> {
        if (!assetName.test(filename)) {
            return Promise.reject(new Error("Invalid icon name"));
        }
        if (this.imageRevision !== revision) {
            this.icons.clear();
            this.imageRevision = revision;
        }
        const cached = this.icons.get(filename);
        if (cached) {
            return cached;
        }
        const icon = this.fetchIcon(filename).catch((error) => {
            if (this.icons.get(filename) === icon) {
                this.icons.delete(filename);
            }
            throw error;
        });
        this.icons.set(filename, icon);
        return icon;
    }

    private async fetchIcon(filename: string) {
        const response = await request(new URL(`./streamdeck/assets/${filename}`, this.url), this.settings.token);
        const mime = response.headers.get("content-type")?.split(";")[0] ?? "";
        return safeImage(await read(response, 65536), mime, filename);
    }
}

async function request(url: URL, token: string, payload?: object) {
    const headers = new Headers();
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }
    if (payload) {
        headers.set("Content-Type", "application/json");
    }
    const response = await fetch(url, {
        headers, method: payload ? "POST" : "GET", body: payload ? JSON.stringify(payload) : undefined,
        redirect: "error", signal: AbortSignal.timeout(3000)
    });
    if (!response.ok) {
        await response.body?.cancel();
        throw new HttpError(response.status);
    }
    return response;
}

async function json(response: Response): Promise<unknown> {
    if (response.headers.get("content-type")?.split(";")[0] !== "application/json") {
        await response.body?.cancel();
        throw new Error("Expected JSON from AetherOnePi");
    }
    return JSON.parse((await read(response, 65536)).toString("utf8"));
}

async function read(response: Response, limit: number) {
    const reader = response.body?.getReader();
    if (!reader) {
        throw new Error("Missing response body");
    }
    const chunks: Uint8Array[] = [];
    let total = 0;
    try {
        while (true) {
            const { value, done } = await reader.read();
            if (done) {
                return Buffer.concat(chunks, total);
            }
            total += value.byteLength;
            if (total > limit) {
                throw new Error("Response is too large");
            }
            chunks.push(value);
        }
    } finally {
        await reader.cancel();
    }
}
