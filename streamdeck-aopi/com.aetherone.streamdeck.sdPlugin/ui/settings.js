const element = (id) => document.getElementById(id);
let socket;
let context;
let action;

function send(payload) {
    if (socket?.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ event: "sendToPlugin", context, action, payload }));
    }
}

window.connectElgatoStreamDeckSocket = (port, uuid, registerEvent, _info, actionInfo) => {
    context = uuid;
    const info = JSON.parse(actionInfo);
    action = info.action;
    element("slotIndex").value = info.payload.settings?.slotIndex ?? "";
    socket = new WebSocket(`ws://127.0.0.1:${port}`);
    socket.addEventListener("open", () => {
        socket.send(JSON.stringify({ event: registerEvent, uuid }));
        send({ type: "status" });
    });
    socket.addEventListener("message", (event) => {
        const message = JSON.parse(event.data);
        if (message.event !== "sendToPropertyInspector") {
            return;
        }
        const { status, url, hasToken, pollInterval } = message.payload;
        if (status) element("status").textContent = status;
        if (url) {
            element("url").value = url;
            element("token").placeholder = hasToken ? "Saved · leave empty to keep" : "Optional for localhost";
            element("pollInterval").value = pollInterval ?? "";
        }
    });
    socket.addEventListener("close", () => {
        element("status").textContent = "Stream Deck connection closed.";
    });
};

element("connection").addEventListener("submit", (event) => {
    event.preventDefault();
    send({
        type: "connect", url: element("url").value, token: element("token").value,
        clearToken: element("clearToken").checked,
        pollInterval: element("pollInterval").valueAsNumber || undefined
    });
    element("token").value = "";
    element("clearToken").checked = false;
});

element("home").addEventListener("click", () => send({ type: "home" }));
element("slotSettings").addEventListener("submit", (event) => {
    event.preventDefault();
    send({ type: "slot", slotIndex: element("slotIndex").value === "" ? undefined : element("slotIndex").valueAsNumber });
});
