# Stream Deck development

AetherOnePi is a single Maven module targeting Java 24. Its existing Javalin 6.6 listener serves the web UI, permissive CORS routes and the analysis WebSocket. Those routes have their own compatibility requirements. The Stream Deck integration starts a separate Javalin listener, with a fixed configurable port and its own network checks.

Processing owns the analysis results, current catalog, embedded broadcast queue and drawable elements. The draw loop consumes bounded remote commands and publishes immutable snapshots. The current-catalog analysis operation performs its existing analysis, session and WebSocket updates on that same thread. The GUI's CLEAR action is a UV visual effect. Stream Deck's analysis clear resets only the current result and GV. Rate controls select an entry from the current analysis for broadcasting.

The embedded broadcast implementation owns queuing, progress, stopping and the safety setting. Remote starts require embedded mode, the safety setting enabled, auto mode off and an idle broadcast queue. Separate broadcast windows lack a shared stop and progress interface. Their control and a remotely writable safety switch are omitted.

Menu and asset changes are loaded from bounded JSON and image files. The plugin uses one bootstrap URL, HTTP polling and two generic action types. State publication is independent of the legacy analysis WebSocket.

## Build and package

Use Java 24 for AetherOnePi and Node 24 for the plugin. This build uses Elgato SDK 3.0.4 and CLI 1.10.4 through the lockfile.

```sh
npm ci
npm run build
npm test
npm run validate -- --no-update-check
npm run pack -- --no-update-check --force
```

The installer is written to `dist/com.aetherone.streamdeck.streamDeckPlugin`. With the project's Maven dependencies already installed locally, run the focused JUnit 5 tests from the project root.

```sh
mvn -Dtest=StreamDeckServerTest resources:resources compiler:compile compiler:testCompile org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test
```

## Menu and asset configuration

Copy a built-in file from `src/main/resources/streamdeck/menus` to `config/streamdeck/menus` under the application working directory. To override the root, pass `-Daopi.streamdeck.config=/path/to/streamdeck` before `-jar`. Place icons in the corresponding `assets` subdirectory. The server keeps the most recent valid catalog if an edit is invalid. A revision change causes each connected device to reload its current menu and icons.

The shared menu supports up to six keys and uses compact positions when a smaller device cannot show the specified coordinates. Add `<menu>.plus-xl.json`, `<menu>.plus.json`, `<menu>.xl.json` or `<menu>.key-only.json` under `menus` for full per-device layouts. Keep the internal menu ID the same. Every non-root menu needs an enabled BACK or HOME key. BROADCAST needs an enabled STOP key. Use the bundled icons as templates for the restricted SVG format. PNGs must be at most 512 × 512 pixels. Each image is limited to 64 KiB.

Encoder rotation sends signed `deltaSteps` in the range −20 to 20. Duration moves by 10 seconds per step and is clamped to 10–3600 seconds. The plugin batches rotation for 110 ms, and the server rate-limits rotation requests. Command POSTs return a job ID and the latest job statuses appear in state. The plugin briefly polls faster after a command to display the confirmed value. Reusing a request ID with the same payload does not repeat its side effect during the five-minute deduplication window.

A modal GUI dialog can pause Processing's draw thread. Commands expire after five seconds in the queue, and the listener rejects new commands when the animation thread is paused. STOP keeps priority and is applied as soon as the animation thread resumes.

## Profiles and verification

The bundled + XL export was imported and exported in Stream Deck 7.6.1 on Windows with a connected + XL. The export contains 36 Keypad slots in a 9 × 4 grid and six Encoder slots with persisted coordinate-aligned indices. Its original device UUID was removed for portability. The connected device's canvas displayed live server titles, icons and dial feedback. The Java API was exercised against isolated local analysis data through start, GV, rate selection, duration adjustment, broadcast start and STOP.

For another model, place the two generic action types on the matching device as described in [INSTALL.md](INSTALL.md), export it, and put the result in the plugin's `profiles` directory. In `manifest.json`, add the extensionless path to `Profiles` with `AutoInstall: true` and the official device type. Stream Deck + uses 7, XL uses 2 and + XL uses 13. Import the export on that model before bundling.

Verify an installation by opening MAIN, navigating to BROADCAST, rotating the duration dial three clockwise steps, and returning via BACK. Select a test analysis result and verify broadcast progress and STOP. Change an icon or label in the override directory and check that the device picks it up. Then close AetherOnePi and confirm the display goes offline and reconnects after restart.

The runtime reports active ESP32 or Infinite USB TRNG sources where available. Aggregate Hotbits counts, separate broadcast windows and remotely writable safety or auto mode settings are outside this implementation.
