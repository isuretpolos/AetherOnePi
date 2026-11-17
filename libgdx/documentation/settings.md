# Settings and live font size

Open **SETTINGS** in the top navigation. The page shares its window, theme, and
font with the dashboard. F11 and Escape continue to control fullscreen.

## Readable text on small, high-resolution displays

The font-size slider ranges from **14 to 32 logical pixels**, in one-pixel steps.
The default is **16 px**. Drag it to see the chosen size immediately in labels,
buttons, navigation, settings descriptions, and the preview sentence. Return to
Dashboard to see the same size there. The setting survives an application restart.

After clicking the slider, arrow keys change the size by one step. Home chooses
14 px and End chooses 32 px. **RESET FONT SIZE** returns to 16 px without changing
the other settings.

The application bundles Noto Sans under the SIL Open Font License. FreeType
renders new glyphs at the selected size, accounting for framebuffer density.
Text is not enlarged from the original small bitmap. No system font installation
or runtime font download is required. The font and license are in
`core/src/main/resources/fonts/`.

The same slider actor remains alive while fonts change, preserving a drag in
progress. Label caches and layout measurements are refreshed on the render
thread. The largest font leaves the settings in a scrollable page; navigation
uses one horizontally scrollable row. Dashboard columns stack earlier when
larger text would otherwise make them too narrow.
The navigation row reserves its preferred height, so switching to the long
Settings page cannot compress its buttons or remove their padding.

## Original AOPi options

All 15 original boolean keys and constructor defaults were reimplemented:

| Group | Key | Default |
| --- | --- | --- |
| Broadcasting | `broadcast.delta.time` | Off |
| Broadcasting | `broadcast.single.rates.only` | Off |
| Broadcasting | `broadcast.embedded` | On |
| Broadcasting | `broadcast.image.layers` | Off |
| Broadcasting | `dynamic.adjustments` | Off |
| Broadcasting | `power.switch` | Off |
| Broadcasting | `safety.switch` | On |
| Broadcasting | `notification.after.broadcast` | Off |
| Analysis | `analysis.very.high.max.hit` | Off |
| Analysis | `watchlist.activate` | Off |
| Sound | `play.binaural.sound` | Off |
| Sound | `play.sound.advanced` | Off |
| Hardware | `trng.infinity` | Off |
| Hardware | `trng.esp32` | Off |
| Appearance | `use.overlay` | On |

Each row has a readable label, explanation, stable key, and ON/OFF control.
The control sits on the left beside its text. Subtly alternating backgrounds
span each complete setting row to make that association clear at every font size.
These options are stored now so future feature implementations can use the same
settings model. Enabling ESP32 or Infinite Noise permits manual acquisition for
that source on the Hotbits page; it does not open a device automatically.
Disabling the corresponding setting stops an active session.
The analysis high-hit option selects a scoring threshold of 1000 instead of 100
on the next analysis. Other future options do not yet start watchlist analysis,
sound, or broadcasting.
The overlay option refers to the future broadcast visualization, not the dark
dashboard background. The font-size setting is already active throughout the UI.

## Saving and failures

Settings are saved in `aopi-libgdx-data/settings.json` under the application's
working directory, or the directory chosen by the `aopi.dataDirectory` JVM
property. This is separate from original AOPi settings. Nothing is imported
automatically from `config/Settings.json`.

Example:

```json
{
  "fontSize": 24,
  "booleans": {
    "broadcast.embedded": true,
    "safety.switch": true,
    "power.switch": false
  }
}
```

Missing options use their defaults. Font sizes outside the supported range are
clamped. Unrecognized keys are preserved when saving, to allow future additions.
The full set of known boolean values is written after a user change.

## Remembering the window

On normal close, the app saves the last window position, content size, maximized
state, and fullscreen state in the same `settings.json`:

```json
"window": {
  "x": 120,
  "y": 100,
  "width": 1100,
  "height": 700,
  "maximized": false,
  "fullscreen": false
}
```

Coordinates are desktop coordinates, so a monitor to the left of the primary
monitor can have negative `x` values. Width and height describe the normal
window's content area, excluding operating-system borders. Fullscreen and
maximized windows keep these normal bounds so returning to windowed mode remains
useful. Closing while minimized preserves the last usable placement.

Startup reads the file before creating the window. Saved bounds are checked
against connected monitors' work areas, with space for window decorations.
Off-screen placement is moved back onto a connected display and oversized bounds
are reduced to fit. Invalid or missing window data uses the default 1285 × 721
window, centered and fitted to the display. The usual 800 × 600 minimum is reduced
if the connected display cannot accommodate it.

**Manual reset:** close the app first, then delete just the `window` JSON member
to reset placement while keeping your font size and feature settings. Or delete
`aopi-libgdx-data/settings.json` entirely to reset all settings. A new file is
written on the next normal close. If you delete the whole JSON member manually,
remove its adjacent comma where needed so the remaining file stays valid JSON.
Deleting the file while the app is running will not reset the current in-memory
settings; closing it writes them again.

Normal close saves settings even when no Settings-page control was changed.
A forced process termination or crash cannot guarantee a final save.

## Write behavior

The preview updates on the next frame. Saving waits until dragging stops and
changes have settled for 0.4 seconds, avoiding writes for every slider step.
Pending changes are also saved on exit. Saving writes a temporary file and then
replaces the destination atomically where supported. A write failure leaves the
previous file intact and displays an error on the page. Another change retries
the save.

If saved JSON cannot be read, defaults are displayed with a visible warning.
Loading or merely opening Settings does not write the file. Normal close writes
the current settings and window placement; if loading failed, defaults replace
the invalid file at that point. The Settings page displays this warning.

## Implementation guide

- `ApplicationSettings`: font bounds and typed boolean values; no UI or disk access.
- `Setting`: stable keys, defaults, grouping, titles, and descriptions.
- `SettingsRepository`: JSON loading and safe replacement of the settings file.
- `SettingsPage`: controls, preview, debounce, and visible save status.
- `UiFontManager`: FreeType glyph generation and label/layout refresh.
- `WindowPlacement`: normal bounds, display modes, and pure monitor-fit rules.
- `DesktopWindowController`: native sampling and restoring of desktop geometry.
- `AopiScreen`: shared stage, navigation, resources, and current-page container.

New pages should use the shared skin and application settings, avoid fixed text
heights, wrap paragraphs, and place long content in scroll panes. They should
not introduce separate font-size preferences or read the original project's
configuration.

## Verification

The Gradle build, installed distribution, and application startup were checked
on Windows/JDK 24.0.1. Eighty-two tests pass, including five dashboard tests, eight
settings tests, five window-placement tests, fourteen core Hotbits tests, and
fourteen desktop Infinite Noise protocol/transport tests, nine case persistence
tests, and seven cached-draw/Essential Questions tests.
Settings tests cover original defaults, persistence across a
restart, font bounds, unknown keys, malformed input, and retaining the old file
when a staging write fails. Window tests cover connected negative-coordinate
monitors, missing monitors, enormous sizes, and displays smaller than the normal
minimum window size.

The optional `:lwjgl3:windowPersistenceSmokeTest` Gradle task briefly opens three
real desktop windows using isolated files under `lwjgl3/build/`. It checks the
actual close/reopen lifecycle, preservation of font and boolean values, reopening
in fullscreen, and returning to the saved normal bounds. It has passed on Windows.
It is separate from `build` because it requires a desktop and OpenGL and changes
display mode briefly. It does not touch the user's application settings.

The separate `:lwjgl3:navigationLayoutSmokeTest` task verifies that navigation
buttons retain their full preferred height and identical dimensions when
switching between Dashboard and Settings. It covers 14, 24, and 32 px fonts and
an 800 × 600 window using isolated test data. This check has passed on Windows.

Interactive verification still needed:

1. Drag the slider slowly from 14 to 32, and back; text should follow without a restart.
2. Release the slider; confirm the saved message and the same size after restart.
3. Visit Dashboard; check that all text uses the selected size.
4. Use 32 px at 800 × 600 and in fullscreen; check scrolling and readable controls.
5. Toggle an option, restart, and confirm it retains its state.
6. Reset the font; verify the other options are unchanged.
